package cn.veasion.project.utils;

import org.springframework.beans.BeanUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CollectionUtils
 *
 * @author luozhuowei
 * @date 2022/11/1
 */
public class CommonUtils {

    public static <T> T copy(Object object, Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            BeanUtils.copyProperties(object, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> copy(List<?> list, Class<T> clazz) {
        List<T> result = new ArrayList<>(list.size());
        try {
            T instance;
            for (Object object : list) {
                instance = clazz.newInstance();
                BeanUtils.copyProperties(object, instance);
                result.add(instance);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

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

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(Object object) {
        return !isEmpty(object);
    }

    public static <T, U extends Comparable<? super U>> void sort(List<T> list, Function<? super T, ? extends U> field, boolean nullInLast) {
        list.sort((o1, o2) -> {
            U value1 = field.apply(o1);
            U value2 = field.apply(o2);
            if (value1 == null) {
                return nullInLast ? 1 : -1;
            } else if (value2 == null) {
                return nullInLast ? -1 : 1;
            }
            if (value1 instanceof String || value2 instanceof String) {
                return new StringComparator().compare(value1.toString(), value2.toString());
            } else {
                return value1.compareTo(value2);
            }
        });
    }

    public static <T> List<T> asList(T... obj) {
        return new ArrayList<>(Arrays.asList(obj));
    }

    public static MapBuilder<String, Object> buildMap() {
        return new MapBuilder<>();
    }

    public static <K, V> Map<K, V> singletonMap(K k1, V v1) {
        return Collections.singletonMap(k1, v1);
    }

    public static <K, V> Map<K, V> buildMap(K k1, V v1) {
        MapBuilder<K, V> builder = new MapBuilder<>();
        return builder.put(k1, v1).build();
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

        public MapBuilder<K, V> exec(Consumer<Map<K, V>> consumer) {
            consumer.accept(map);
            return this;
        }

        public Map<K, V> build() {
            return map;
        }
    }

}
