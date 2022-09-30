package cn.veasion.project.config;

import cn.veasion.project.aspect.IgnoreCompanyIdAspect;
import cn.veasion.project.aspect.LimitAspect;
import cn.veasion.project.dao.CommonDao;
import cn.veasion.project.dao.ReadWriteTransactionInterceptor;
import cn.veasion.project.service.CacheServiceImpl;
import cn.veasion.project.utils.SpringBeanUtils;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Import({SpringBeanUtils.class, IgnoreCompanyIdAspect.class})
public class ProjectAutoConfiguration {

    @Configuration
    @Import({CacheServiceImpl.class, LimitAspect.class})
    public static class CacheConfiguration {
    }

    @Bean
    public CommonDao commonDao(DataSource dataSource) {
        return new CommonDao(dataSource);
    }

    @Bean
    public DefaultBeanFactoryPointcutAdvisor defaultBeanFactoryPointcutAdvisor(TransactionManager transactionManager, AspectJExpressionPointcut servicePointcut) {
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
