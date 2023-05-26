package cn.veasion.project.config;

import cn.veasion.project.aspect.IgnoreCompanyIdAspect;
import cn.veasion.project.aspect.LimitAspect;
import cn.veasion.project.aspect.LogAspect;
import cn.veasion.project.dao.CommonDao;
import cn.veasion.project.dao.ReadWriteDataSource;
import cn.veasion.project.dao.ReadWriteTransactionInterceptor;
import cn.veasion.project.service.CacheServiceImpl;
import cn.veasion.project.utils.SpringBeanUtils;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;

/**
 * ProjectAutoConfiguration
 *
 * @author luozhuowei
 * @date 2022/9/30
 */
@Configuration
@Import({SpringBeanUtils.class, IgnoreCompanyIdAspect.class, LogAspect.class})
public class ProjectAutoConfiguration {

    @Configuration
    @ConditionalOnClass({RedisTemplate.class})
    @Import({CacheServiceImpl.class, LimitAspect.class})
    public static class CacheConfiguration {
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "dataSource")
    public ReadWriteDataSource dataSource(@Qualifier("readDataSource") DataSource readDataSource,
                                          @Qualifier("writeDataSource") DataSource writeDataSource) {
        ReadWriteDataSource readWriteDataSource = new ReadWriteDataSource();
        readWriteDataSource.setRead(readDataSource);
        readWriteDataSource.setWrite(writeDataSource);
        return readWriteDataSource;
    }

    @Bean
    @ConditionalOnMissingBean(name = "commonDao")
    public CommonDao commonDao(DataSource dataSource) {
        return new CommonDao(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(name = "defaultBeanFactoryPointcutAdvisor")
    public DefaultBeanFactoryPointcutAdvisor defaultBeanFactoryPointcutAdvisor(TransactionManager transactionManager,
                                                                               @Qualifier("servicePointcut") AspectJExpressionPointcut servicePointcut) {
        DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();

        NameMatchTransactionAttributeSource source = new NameMatchTransactionAttributeSource();
        RuleBasedTransactionAttribute requiredTx = new RuleBasedTransactionAttribute();
        requiredTx.setRollbackRules(Collections.singletonList(new RollbackRuleAttribute(Throwable.class)));
        requiredTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        source.setNameMap(new HashMap<String, TransactionAttribute>() {{
            put("add*", requiredTx);
            put("save*", requiredTx);
            put("insert*", requiredTx);
            put("batchAdd*", requiredTx);
            put("update*", requiredTx);
            put("delete*", requiredTx);
            put("*WithTx", requiredTx);
        }});

        // 使用读写事务拦截动态切数据源
        TransactionInterceptor interceptor = new ReadWriteTransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(source);

        advisor.setAdvice(interceptor);
        advisor.setPointcut(servicePointcut);

        return advisor;
    }

}
