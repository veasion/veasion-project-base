package cn.veasion.project.model;

import cn.hutool.core.bean.BeanUtil;

/**
 * IBaseVO
 *
 * @author luozhuowei
 * @date 2022/8/12
 */
public interface IBaseVO<T> {

    default T convertTo(Class<T> clazz) {
        return BeanUtil.copyProperties(this, clazz);
    }

}
