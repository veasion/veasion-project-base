package cn.veasion.project.dao;

import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.lang.reflect.Method;

/**
 * 读写事务拦截器（根据事务机制读写分离）
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class ReadWriteTransactionInterceptor extends TransactionInterceptor {

    @Override
    protected Object invokeWithinTransaction(Method method, Class<?> targetClass, InvocationCallback invocation) throws Throwable {
        try {
            return super.invokeWithinTransaction(method, targetClass, invocation);
        } finally {
            ReadWriteDataSource.remove();
        }
    }

    @Override
    protected TransactionManager determineTransactionManager(TransactionAttribute txAttr) {
        if (txAttr == null) {
            ReadWriteDataSource.switchRead();
        } else if (txAttr.isReadOnly()) {
            ReadWriteDataSource.switchRead();
        } else {
            ReadWriteDataSource.switchWrite();
        }
        return super.determineTransactionManager(txAttr);
    }

}
