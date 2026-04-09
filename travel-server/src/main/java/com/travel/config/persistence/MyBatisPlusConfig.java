package com.travel.config.persistence;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>
 * 负责统一注册 Mapper 扫描和分页插件。
 */
@Configuration
@MapperScan("com.travel.mapper")
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器，并启用 MySQL 分页插件。
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 统一在基础设施层开启分页拦截，避免各查询服务手动拼装分页限制。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
