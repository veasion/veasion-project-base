package cn.veasion.project.dao;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.function.Supplier;

/**
 * DataSourceUtils
 *
 * @author luozhuowei
 * @date 2022/6/28
 */
public class DataSourceUtils {

    /**
     * 编程式事务
     *
     * @param dataSourceTransactionManager 数据源事务管理器
     * @param supplier                     执行方法
     * @return 返回结果
     */
    public static <T> T withTx(DataSourceTransactionManager dataSourceTransactionManager, Supplier<T> supplier) {
        String oldKey = ReadWriteDataSource.switchWrite();
        DefaultTransactionDefinition def = new DefaultTransactionAttribute();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(def);
        T result;
        try {
            result = supplier.get();
            dataSourceTransactionManager.commit(transactionStatus);
            return result;
        } catch (Exception e) {
            dataSourceTransactionManager.rollback(transactionStatus);
            throw e;
        } finally {
            ReadWriteDataSource.THREAD_LOCAL.set(oldKey);
        }
    }

}
