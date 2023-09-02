package cn.veasion.project.model;

import com.alibaba.fastjson.JSON;

/**
 * IBaseVO
 *
 * @author luozhuowei
 * @date 2022/8/12
 */
public interface IBaseVO<T> {

    default T convertTo(Class<T> clazz) {
        return JSON.parseObject(JSON.toJSONString(this), clazz);
    }

}
