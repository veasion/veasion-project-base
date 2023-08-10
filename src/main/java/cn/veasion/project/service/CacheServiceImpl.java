package cn.veasion.project.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CacheServiceImpl
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
@Service
public class CacheServiceImpl implements CacheService {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void set(String key, Object value, Long time, TimeUnit timeUnit) {
        if (time != null) {
            redisTemplate.opsForValue().set(key, value, time, timeUnit);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    @Override
    public <R> R get(String key) {
        return tryGetValue(key);
    }

    @Override
    public void pushHash(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getHash(String key, String hashKey) {
        return (R) redisTemplate.opsForHash().get(key, hashKey);
    }

    @Override
    public <R> R loadCache(String key, Supplier<R> supplier, Long time, TimeUnit timeUnit, boolean refresh) {
        R value = refresh ? null : tryGetValue(key);
        if (value == null) {
            value = supplier.get();
            if (time != null) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
        }
        return value;
    }

    @Override
    public <R> R loadHashCache(String mainKey, String key, Supplier<R> supplier, boolean refresh) {
        R value = refresh ? null : tryHashValue(mainKey, key);
        if (value == null) {
            value = supplier.get();
            try {
                redisTemplate.opsForHash().put(mainKey, key, value);
            } catch (Exception e) {
                log.error("写入缓存失败：{} {}", mainKey, key, e);
            }
        }
        return value;
    }

    @Override
    public boolean tryLock(String key, Runnable runnable, int waitTime, int leaseTime, TimeUnit timeUnit) {
        RLock lock = null;
        boolean hasLock;
        try {
            lock = redissonClient.getLock(key);
            hasLock = lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (Exception e) {
            log.error("获取锁失败", e);
            // 不影响业务
            hasLock = true;
        }
        if (hasLock) {
            try {
                runnable.run();
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public <R> R lock(String key, Supplier<R> supplier, int time, TimeUnit timeUnit, boolean tryLock) {
        RLock lock = null;
        try {
            if (tryLock) {
                try {
                    lock = redissonClient.getLock(key);
                    lock.lock(time, timeUnit);
                } catch (Exception e) {
                    // redis 失败不影响业务，继续进行
                    log.error("获取锁失败", e);
                }
            } else {
                lock = redissonClient.getLock(key);
                lock.lock(time, timeUnit);
            }
            return supplier.get();
        } finally {
            if (lock != null && lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    @Override
    public <R> R multiLock(String prefix, List<String> keys, boolean skipNull, Supplier<R> supplier, int time, TimeUnit timeUnit) {
        Stream<String> stream = skipNull ? keys.stream().filter(Objects::nonNull) : keys.stream();
        List<RLock> locks = stream.distinct().map(key -> redissonClient.getLock(CacheService.buildKey(prefix, key))).collect(Collectors.toList());
        if (locks.isEmpty()) {
            return supplier.get();
        }
        RLock lock = redissonClient.getMultiLock(locks.toArray(new RLock[]{}));
        try {
            lock.lock(time, timeUnit);
            return supplier.get();
        } finally {
            try {
                lock.unlock();
            } catch (Exception e) {
                locks.stream().filter(RLock::isLocked).forEach(RLock::unlock);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <R> R tryGetValue(String key) {
        try {
            return (R) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("加载缓存失败：key = " + key, e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <R> R tryHashValue(String mainKey, String key) {
        try {
            return (R) redisTemplate.opsForHash().get(mainKey, key);
        } catch (Exception e) {
            log.error("加载缓存失败：mainKey = {}, key = {}", mainKey, key, e);
        }
        return null;
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    @Override
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, time, timeUnit));
    }

    @Override
    public boolean expireAt(String key, Date date) {
        return Boolean.TRUE.equals(redisTemplate.expireAt(key, date));
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Long incr(String key, long l) {
        return redisTemplate.opsForValue().increment(key, l);
    }

    @Override
    public Long incrHash(String mainKey, String key, long l) {
        return redisTemplate.opsForHash().increment(mainKey, key, l);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void deleteHash(String mainKey, String key) {
        redisTemplate.opsForHash().delete(mainKey, key);
    }

    @Override
    public List<String> scanKeys(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
        RedisConnection connection = Objects.requireNonNull(factory).getConnection();
        List<String> keys = new ArrayList<>();
        try {
            Cursor<byte[]> cursor = connection.scan(options);
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            RedisConnectionUtils.releaseConnection(connection, factory);
        }
        return keys;
    }

    @Override
    public void scanDelete(String pattern) {
        List<String> keys = scanKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

}
