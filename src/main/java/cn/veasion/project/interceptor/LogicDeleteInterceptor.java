package cn.veasion.project.interceptor;

import cn.veasion.project.model.ILogicDelete;

/**
 * 逻辑删除拦截器
 *
 * @author luozhuowei
 * @date 2021/12/18
 */
public class LogicDeleteInterceptor extends cn.veasion.db.interceptor.LogicDeleteInterceptor {

    public LogicDeleteInterceptor() {
        super("isDeleted", 0, 1);
    }

    @Override
    protected boolean containSkipClass(Class<?> clazz) {
        if (!ILogicDelete.class.isAssignableFrom(clazz)) {
            return true;
        }
        return super.containSkipClass(clazz);
    }

}
