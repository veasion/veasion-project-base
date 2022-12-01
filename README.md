# veasion-project-base

veasion-project-base 是一个基于veasion-db和veasion-db-mybatis、springboot 搭建的项目基础框架，用于项目快速开发。

完成可以快速低代码生成配置一套CURD无需写任何代码。


## maven 依赖
添加 veasion-project-base 依赖
```xml
<dependency>
    <groupId>cn.veasion</groupId>
    <artifactId>veasion-project-base</artifactId>
    <version>1.0.8</version>
</dependency>
```

## 接入配置示例

注意下面的包名 cn.xxx 替换成自己项目的实际包名

### 数据源配置
```java
import cn.veasion.db.mybatis.MybatisMapperFactoryBean;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * DaoConfig
 *
 * @author luozhuowei
 */
@Configuration
@PropertySource(name = "jdbc", value = {DaoConfig.READ_CONFIG, DaoConfig.WRITE_CONFIG})
@MapperScan(basePackages = "com.xxx.dao", factoryBean = MybatisMapperFactoryBean.class)
public class DaoConfig {

    static final String READ_CONFIG = "classpath:config/${spring.profiles.active}/jdbc.read.properties";
    static final String WRITE_CONFIG = "classpath:config/${spring.profiles.active}/jdbc.write.properties";

    private static final String TX_EXPRESSION = "execution(* com.xxx.service..*.*(..))";

    @Bean(destroyMethod = "close")
    @ConfigurationProperties(prefix = "jdbc.write")
    public DruidDataSource writeDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean(destroyMethod = "close")
    @ConfigurationProperties(prefix = "jdbc.read")
    public DruidDataSource readDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean
    public AspectJExpressionPointcut servicePointcut() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(TX_EXPRESSION);
        return pointcut;
    }

}
```

### 异步配置
```java
import cn.veasion.project.session.DelegatingSessionExecutorService;
import cn.veasion.project.utils.SpringBeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AsyncConfig
 *
 * @author luozhuowei
 */
@Configuration
@EnableScheduling
public class AsyncConfig extends AsyncConfigurerSupport {

    @Bean
    @Primary
    public ExecutorService executorService() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        return new DelegatingSessionExecutorService(new ThreadPoolExecutor(
                corePoolSize,
                60,
                8,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(256),
                new CustomizableThreadFactory("async-pool-"),
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    @Override
    public Executor getAsyncExecutor() {
        // return executorService();
        return SpringBeanUtils.getBean(ExecutorService.class);
    }

}
```

### 统一会话Session配置
其他地方通过 SessionHelper 类获取当前用户信息，如 SessionHelper.getSessionUser() 
```java
import cn.veasion.project.session.ISessionProvider;
import cn.veasion.project.session.ISessionUser;

public class SessionProvider implements ISessionProvider {

    @Override
    public ISessionUser getSessionUser() {
        // TODO 这里返回当前登录用户的信息
        return null;
    }

}
```

SPI 文件 cn.veasion.project.session.ISessionProvider 加入当前实现类 com.xxx.SessionProvider

## 使用示例

### mapper
```java
import cn.veasion.db.jdbc.EntityDao;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends EntityDao<SysUserPO, String> {
    // 这里基础通用CURD，下面可以自定义mybatis的方法（一般来说使用了veasion-db后mybatis完全可以废弃了）
}
```

### service
```java
import cn.veasion.project.service.BaseService;

public interface SysUserService extends BaseService<SysUserVO, SysUserPO, String> {

}
```
```java
import org.springframework.stereotype.Service;
import cn.veasion.project.service.BaseServiceImpl;
import javax.annotation.Resource;

@Service
public class SysUserServiceImpl extends BaseServiceImpl<SysUserVO, SysUserPO, String> implements SysUserService {
    
    @Resource
    private SysUserMapper sysUserMapper;

    @Override
    protected EntityDao<SysUserPO, String> getEntityDao() {
        return sysUserMapper;
    }

}
```

### controller
```java
import cn.veasion.project.model.QueryCriteria;
import cn.veasion.project.aspect.Log;
import cn.veasion.project.model.Page;
import cn.veasion.project.model.R;
import cn.veasion.project.utils.FileUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "系统用户")
@RequestMapping("/sysUser")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @GetMapping("/queryById")
    @ApiOperation("根据ID查询系统用户")
    public R<SysUserVO> queryById(@RequestParam String id) {
        return R.ok((sysUserService.queryById(id)));
    }

    @PostMapping("/listPage")
    @ApiOperation("分页查询系统用户")
    public Page<SysUserVO> listPage(@RequestBody QueryCriteria criteria) {
        return Page.ok(sysUserService.listPage(criteria));
    }

    @PostMapping("/add")
    @Log("新增系统用户")
    @ApiOperation("新增系统用户")
    public R<Object> add(@Validated @RequestBody SysUserVO obj) {
        return R.ok(sysUserService.add(obj.convertTo(SysUserPO.class)));
    }

    @PostMapping("/update")
    @Log("修改系统用户")
    @ApiOperation("修改系统用户")
    public R<Object> update(@Validated @RequestBody SysUserVO obj) {
        return R.ok(sysUserService.updateById(obj.convertTo(SysUserPO.class)));
    }

    @PostMapping("/delete")
    @Log("删除系统用户")
    @ApiOperation("删除系统用户")
    public R<Object> delete(@RequestBody List<String> ids) {
        return R.ok(sysUserService.deleteByIds(ids));
    }

    @ApiOperation("导出数据")
    @PostMapping("/download")
    public void download(@RequestBody QueryCriteria criteria, HttpServletResponse response) throws IOException {
        download(sysUserService.list(criteria), response);
    }

    private void download(List<SysUserVO> list, HttpServletResponse response) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUserVO sysUser : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("用户名", sysUser.getUsername());
            map.put("手机号", sysUser.getMobile());
            map.put("昵称", sysUser.getNickName());
            map.put("头像地址", sysUser.getAvatarUrl());
            result.add(map);
        }
        FileUtil.downloadExcel(result, response);
    }

}
```
## 赞助

项目的发展离不开您的支持，请作者喝杯咖啡吧~

ps：辣条也行 ☕

![支付宝](https://veasion.oss-cn-shanghai.aliyuncs.com/alipay.png?x-oss-process=image/resize,m_lfit,h_360,w_360)
