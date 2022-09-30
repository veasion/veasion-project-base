package cn.veasion.project.dao;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 读写动态数据源
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class ReadWriteDataSource extends AbstractRoutingDataSource {

    public static final String READ = "read";
    public static final String WRITE = "write";
    public static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    private final String defaultKey;
    private Map<Object, Object> targetDataSources;

    public ReadWriteDataSource() {
        this.defaultKey = WRITE;
    }

    public ReadWriteDataSource(String defaultKey) {
        this.defaultKey = Objects.requireNonNull(defaultKey);
    }

    public void setRead(DataSource dataSource) {
        put(READ, dataSource);
    }

    public void setWrite(DataSource dataSource) {
        put(WRITE, dataSource);
    }

    public void put(String key, DataSource dataSource) {
        if (targetDataSources == null) {
            targetDataSources = new HashMap<>();
        }
        targetDataSources.put(key, dataSource);
        setTargetDataSources(targetDataSources);
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.targetDataSources = targetDataSources;
        super.setTargetDataSources(targetDataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String key = THREAD_LOCAL.get();
        return key != null ? key : defaultKey;
    }

    public static String switchRead() {
        String key = THREAD_LOCAL.get();
        THREAD_LOCAL.set(READ);
        return key;
    }

    public static String switchWrite() {
        String key = THREAD_LOCAL.get();
        THREAD_LOCAL.set(WRITE);
        return key;
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

}
