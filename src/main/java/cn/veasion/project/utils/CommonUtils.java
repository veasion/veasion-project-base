package cn.veasion.project.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * CollectionUtils
 *
 * @author luozhuowei
 * @date 2022/11/1
 */
public class CommonUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(Object object) {
        boolean isEmpty = false;
        if (object == null) {
            isEmpty = true;
        } else if (object instanceof String) {
            isEmpty = isEmpty((String) object);
        } else if (object instanceof Collection) {
            isEmpty = isEmpty((Collection<?>) object);
        } else if (object instanceof Map) {
            isEmpty = isEmpty((Map<?, ?>) object);
        } else if (object instanceof Object[]) {
            isEmpty = isEmpty((Object[]) object);
        } else if (object.getClass().isArray()) {
            isEmpty = Array.getLength(object) == 0;
        }
        return isEmpty;
    }

    public static MapBuilder<String, Object> buildMap() {
        return new MapBuilder<>();
    }

    public static <K, V> Map<K, V> singletonMap(K k1, V v1) {
        return Collections.singletonMap(k1, v1);
    }

    public static <K, V> Map<K, V> buildMap(K k1, V v1, K k2, V v2) {
        MapBuilder<K, V> builder = new MapBuilder<>();
        return builder.put(k1, v1).put(k2, v2).build();
    }

    public static <K, V> Map<K, V> buildMap(K k1, V v1, K k2, V v2, K k3, V v3) {
        MapBuilder<K, V> builder = new MapBuilder<>();
        return builder.put(k1, v1).put(k2, v2).put(k3, v3).build();
    }

    public static class MapBuilder<K, V> {
        private Map<K, V> map = new HashMap<>();

        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public Map<K, V> build() {
            return map;
        }
    }

}
