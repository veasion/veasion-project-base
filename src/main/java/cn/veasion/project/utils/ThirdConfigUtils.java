package cn.veasion.project.utils;

import cn.veasion.db.utils.TypeUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * ThirdConfigUtils
 *
 * @author luozhuowei
 * @date 2022/1/20
 */
public class ThirdConfigUtils {

    private final static Logger LOGGER = LoggerFactory.getLogger(ThirdConfigUtils.class);

    public static String CHARSET = null;
    public static String DEFAULT_PATH = "third.properties";
    public static String EXT_PATH_KEY = "extConfigPath";
    private static final Map<String, Set<String>> filenamePathMap;
    private static final Map<String, Map<String, String>> filenameConfig;

    static {
        filenamePathMap = new HashMap<>();
        filenameConfig = new HashMap<>();
        loadDefault();
    }

    public synchronized static void reload() {
        filenameConfig.clear();
        for (Map.Entry<String, Set<String>> entry : filenamePathMap.entrySet()) {
            for (String path : entry.getValue()) {
                if ("".equals(entry.getKey()) && DEFAULT_PATH.equals(path)) {
                    loadDefault();
                } else {
                    load(entry.getKey(), path);
                }
            }
        }
    }

    private static void loadDefault() {
        Map<String, String> config = load(DEFAULT_PATH);
        if (config == null) {
            return;
        }
        String extConfigPath = config.get(EXT_PATH_KEY);
        if (StringUtils.isNotEmpty(extConfigPath)) {
            try {
                File file = new File(extConfigPath);
                if (!file.exists()) {
                    LOGGER.error("扩展配置不存在：" + extConfigPath);
                    return;
                }
                Properties ext_properties = new Properties();
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    if (CHARSET != null) {
                        ext_properties.load(new InputStreamReader(is, CHARSET));
                    } else {
                        byte[] bytes = IOUtils.toByteArray(is);
                        String charset = FileUtil.autoTextCharset(bytes);
                        ext_properties.load(new InputStreamReader(new ByteArrayInputStream(bytes), charset));
                    }
                }
                if (!ext_properties.isEmpty()) {
                    for (Map.Entry<Object, Object> entry : ext_properties.entrySet()) {
                        config.put(entry.getKey().toString(), entry.getValue() == null ? null : entry.getValue().toString());
                    }
                }
            } catch (Exception e) {
                LOGGER.error("加载扩展配置失败：" + extConfigPath, e);
            }
        }
    }

    public synchronized static Map<String, String> load(String path) {
        return load("", path);
    }

    public synchronized static Map<String, String> load(String filename, String path) {
        try {
            Map<String, String> config = null;
            File file = new File(path);
            InputStream is;
            if (file.exists()) {
                is = Files.newInputStream(file.toPath());
            } else {
                is = ThirdConfigUtils.class.getClassLoader().getResourceAsStream(path);
            }
            try {
                if (is != null) {
                    filenamePathMap.compute(filename, (k, v) -> {
                        v = new HashSet<>();
                        v.add(path);
                        return v;
                    });
                    config = filenameConfig.computeIfAbsent(filename, k -> new HashMap<>());
                    Properties properties = new Properties();
                    if (CHARSET != null) {
                        properties.load(new InputStreamReader(is, CHARSET));
                    } else {
                        byte[] bytes = IOUtils.toByteArray(is);
                        String charset = FileUtil.autoTextCharset(bytes);
                        properties.load(new InputStreamReader(new ByteArrayInputStream(bytes), charset));
                    }
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        String key = entry.getKey().toString();
                        String value = entry.getValue() == null ? null : entry.getValue().toString();
                        config.put(key, value);
                    }
                }
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return config;
        } catch (Exception e) {
            LOGGER.error("加载配置文件失败：" + path, e);
            return null;
        }
    }

    public static String getProperty(String key) {
        return getFilenameProperty("", key);
    }

    public static <T> T getProperty(String key, Class<T> clazz) {
        return TypeUtils.convert(getFilenameProperty("", key), clazz);
    }

    public static void setProperty(String key, String value) {
        setFilenameProperty("", key, value);
    }

    public static String getProperty(String key, String defVal) {
        return getFilenameProperty("", key, defVal);
    }

    public static String getFilenameProperty(String filename, String key) {
        Map<String, String> map = filenameConfig.get(filename);
        return map != null ? map.get(key) : null;
    }

    public static <T> T getFilenameProperty(String filename, String key, Class<T> clazz) {
        return TypeUtils.convert(getFilenameProperty(filename, key), clazz);
    }

    public static void setFilenameProperty(String filename, String key, String value) {
        synchronized (filenameConfig) {
            Map<String, String> map = filenameConfig.get(filename);
            if (map != null) {
                map.put(key, value);
            }
        }
    }

    public static String getFilenameProperty(String filename, String key, String defVal) {
        String property = getFilenameProperty(filename, key);
        return StringUtils.isNotEmpty(property) ? property : defVal;
    }

}
