package com.travel.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全基础配置。
 * <p>
 * 当前主要提供密码加密能力，不启用完整的 Spring Security 认证链路。
 */
@Configuration
public class SecurityConfig {

    /**
     * 注册 BCrypt 密码编码器。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
