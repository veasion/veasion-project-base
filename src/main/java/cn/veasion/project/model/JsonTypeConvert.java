package cn.veasion.project.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * JsonTypeConvert
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public abstract class JsonTypeConvert {

    @Override
    public String toString() {
        return JSON.toJSONString(this, SerializerFeature.WriteMapNullValue);
    }

}
