package cn.veasion.project.interceptor;

import cn.veasion.db.utils.TypeConvert;
import cn.veasion.project.model.JsonTypeConvert;
import cn.veasion.project.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

/**
 * ExtTypeConvert
 *
 * @author luozhuowei
 * @date 2021/12/18
 */
public class ExtTypeConvert implements TypeConvert {

    @Override
    public <T> T convert(Object object, Class<T> clazz) {
        if (object instanceof String && StringUtils.isNotEmpty((String) object)) {
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
            return value.toString();
        }
        return value;
    }

}
