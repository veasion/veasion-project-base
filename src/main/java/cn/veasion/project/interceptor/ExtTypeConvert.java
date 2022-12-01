package cn.veasion.project.interceptor;

import cn.veasion.db.utils.TypeConvert;
import cn.veasion.db.utils.TypeUtils;
import cn.veasion.project.model.IEnum;
import cn.veasion.project.model.JsonTypeConvert;
import cn.veasion.project.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExtTypeConvert
 *
 * @author luozhuowei
 * @date 2021/12/18
 */
public class ExtTypeConvert implements TypeConvert {

    private static final Map<Class<?>, Map<Object, IEnum<?>>> ENUM_VALUE_CACHE = new ConcurrentHashMap<>();

    @Override
    public <T> T convert(Object object, Class<T> clazz) {
        if (clazz.isEnum() && IEnum.class.isAssignableFrom(clazz)) {
            // 把值转换成枚举
            return valueToEnum(object, clazz);
        } else if (object instanceof String && StringUtils.isNotEmpty((String) object)) {
            // 字符串转JSON
            String str = object.toString();
            if (JSON.class.isAssignableFrom(clazz)) {
                return (T) (JSONArray.class.equals(clazz) ? JSON.parseArray(str) : JSON.parseObject(str));
            } else if (JsonTypeConvert.class.isAssignableFrom(clazz)) {
                return JSON.parseObject(str, clazz);
            }
        }
        return null;
    }

    @Override
    public Object convertValue(Object value) {
        if (value instanceof JSON || value instanceof JsonTypeConvert) {
            // JSON转字符串
            return value.toString();
        } else if (value instanceof IEnum) {
            // 把枚举转换成值
            return ((IEnum<?>) value).getValue();
        }
        return value;
    }

    @Override
    public boolean isSimpleClass(Class<?> clazz) {
        return IEnum.class.isAssignableFrom(clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> T valueToEnum(Object object, Class<T> clazz) {
        Map<Object, IEnum<?>> enumMap = ENUM_VALUE_CACHE.get(clazz);
        if (enumMap == null) {
            try {
                Method valuesMethod = clazz.getDeclaredMethod("values");
                valuesMethod.setAccessible(true);
                Object values = valuesMethod.invoke(clazz);
                enumMap = new HashMap<>();
                int length = Array.getLength(values);
                for (int i = 0; i < length; i++) {
                    IEnum<?> iEnum = (IEnum<?>) Array.get(values, i);
                    if (iEnum.getValue() == null) {
                        continue;
                    }
                    enumMap.put(iEnum.getValue(), iEnum);
                }
                ENUM_VALUE_CACHE.put(clazz, enumMap);
            } catch (Exception e) {
                return null;
            }
        }
        T value = (T) enumMap.get(object);
        if (value != null) {
            return value;
        } else if (enumMap.size() > 0) {
            Class<?> valueClass = enumMap.keySet().iterator().next().getClass();
            if (valueClass != object.getClass()) {
                return (T) enumMap.get(TypeUtils.convert(object, valueClass));
            }
        }
        return null;
    }

}
