package cn.veasion.project.service;

import cn.veasion.db.utils.LeftRight;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * CacheService
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public interface CacheService {

    void set(String key, Object value, Long time, TimeUnit timeUnit);

    <R> R get(String key);

    void pushHash(String key, String hashKey, Object value);

    <R> R getHash(String key, String hashKey);

    default <R> R loadCache(String key, Supplier<R> supplier) {
        return loadCache(key, supplier, null, null);
    }

    default <R> R loadCache(String key, Supplier<R> supplier, boolean refresh) {
        return loadCache(key, supplier, null, null, refresh);
    }

    default <R> R loadCache(String key, Supplier<R> supplier, Long time, TimeUnit timeUnit) {
        return loadCache(key, supplier, time, timeUnit, false);
    }

    <R> R loadCache(String key, Supplier<R> supplier, Long time, TimeUnit timeUnit, boolean refresh);

    <R> R loadCacheWithExpireTime(String key, Supplier<LeftRight<R, Date>> supplier, boolean refresh);

    default <R> R loadHashCache(String mainKey, String key, Supplier<R> supplier) {
        return loadHashCache(mainKey, key, supplier, false);
    }

    <R> R loadHashCache(String mainKey, String key, Supplier<R> supplier, boolean refresh);

    default boolean tryLock(String key, Runnable runnable) {
        return tryLock(key, runnable, 0, 1, TimeUnit.MINUTES);
    }

    boolean tryLock(String key, Runnable runnable, int waitTime, int leaseTime, TimeUnit timeUnit);

    default <R> R lock(String key, Supplier<R> supplier) {
        return lock(key, supplier, 1, TimeUnit.MINUTES);
    }

    default <R> R lock(String key, Supplier<R> supplier, int time, TimeUnit timeUnit) {
        return lock(key, supplier, time, timeUnit, true);
    }

    <R> R lock(String key, Supplier<R> supplier, int time, TimeUnit timeUnit, boolean tryLock);

    default <R> R multiLock(String prefix, List<String> keys, boolean skipNull, Supplier<R> supplier) {
        return multiLock(prefix, keys, skipNull, supplier, 1, TimeUnit.MINUTES);
    }

    <R> R multiLock(String prefix, List<String> keys, boolean skipNull, Supplier<R> supplier, int time, TimeUnit timeUnit);

    Long getExpire(String key, TimeUnit timeUnit);

    boolean expire(String key, long time, TimeUnit timeUnit);

    boolean expireAt(String key, Date date);

    boolean hasKey(String key);

    Long incr(String key, long l);

    Long incrHash(String mainKey, String key, long l);

    void delete(String key);

    void deleteHash(String mainKey, String key);

    List<String> scanKeys(String pattern);

    void scanDelete(String pattern);

    RedisTemplate<String, Object> getRedisTemplate();

    static String buildKey(Object... keys) {
        return Arrays.stream(keys).map(String::valueOf).collect(Collectors.joining("_"));
    }

}
