package cn.veasion.project.utils;

import cn.veasion.db.utils.TypeUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ThirdConfigUtils
 *
 * @author luozhuowei
 * @date 2022/1/20
 */
public class ThirdConfigUtils {

    public static String CHARSET = "GBK";
    private static final Map<String, String> config;

    static {
        config = new HashMap<>();
        reload();
    }

    public synchronized static void reload() {
        load("third.properties");
        String extConfigPath = config.get("extConfigPath");
        if (StringUtils.isNotEmpty(extConfigPath)) {
            try {
                File file = new File(extConfigPath);
                if (!file.exists()) {
                    System.err.println("扩展配置不存在：" + extConfigPath);
                    return;
                }
                Properties ext_properties = new Properties();
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    ext_properties.load(new InputStreamReader(is, CHARSET));
                }
                if (ext_properties.size() > 0) {
                    for (Map.Entry<Object, Object> entry : ext_properties.entrySet()) {
                        config.put(entry.getKey().toString(), entry.getValue() == null ? null : entry.getValue().toString());
                    }
                }
            } catch (Exception e) {
                System.err.println("加载扩展配置失败：" + extConfigPath);
                e.printStackTrace();
            }
        }
    }

    public synchronized static boolean load(String name) {
        try {
            try (InputStream is = ThirdConfigUtils.class.getClassLoader().getResourceAsStream(name)) {
                if (is != null) {
                    Properties properties = new Properties();
                    properties.load(new InputStreamReader(is, CHARSET));
                    for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                        config.put(entry.getKey().toString(), entry.getValue() == null ? null : entry.getValue().toString());
                    }
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("加载配置文件失败：" + name);
            e.printStackTrace();
            return false;
        }
    }

    public static String getProperty(String key) {
        return config.get(key);
    }

    public static <T> T getProperty(String key, Class<T> clazz) {
        return TypeUtils.convert(getProperty(key), clazz);
    }

    public synchronized static void setProperty(String key, String value) {
        config.put(key, value);
    }

    public static String getProperty(String key, String defVal) {
        String property = getProperty(key);
        return StringUtils.isNotEmpty(property) ? property : defVal;
    }

}
