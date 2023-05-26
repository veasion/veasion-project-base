package cn.veasion.project.utils;

import cn.veasion.db.utils.TypeUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Properties;

/**
 * ThirdConfigUtils
 *
 * @author luozhuowei
 * @date 2022/1/20
 */
public class ThirdConfigUtils {

    private static final String CHARSET = "GBK";
    private static Properties properties;

    static {
        reload();
    }

    public synchronized static void reload() {
        properties = new Properties();
        try {
            try (InputStream is = ThirdConfigUtils.class.getClassLoader().getResourceAsStream("third.properties")) {
                if (is != null) {
                    properties.load(new InputStreamReader(is, CHARSET));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String extConfigPath = properties.getProperty("extConfigPath");
        if (StringUtils.isNotEmpty(extConfigPath)) {
            try {
                File file = new File(extConfigPath);
                if (!file.exists()) {
                    return;
                }
                Properties ext_properties = new Properties();
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    ext_properties.load(new InputStreamReader(is, CHARSET));
                }
                if (ext_properties.size() > 0) {
                    for (Object key : ext_properties.keySet()) {
                        if (key instanceof String) {
                            properties.put(key, ext_properties.getProperty((String) key));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static <T> T getProperty(String key, Class<T> clazz) {
        return TypeUtils.convert(properties.getProperty(key), clazz);
    }

    public synchronized static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static String getProperty(String key, String defVal) {
        String property = properties.getProperty(key);
        return StringUtils.isNotEmpty(property) ? property : defVal;
    }

}
