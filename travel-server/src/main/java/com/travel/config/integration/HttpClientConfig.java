package com.travel.config.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置。
 * <p>
 * 统一提供服务内部调用第三方接口所需的 RestTemplate。
 */
@Configuration
public class HttpClientConfig {

    /**
     * 注册 RestTemplate。
     * <p>
     * 第三方调用入口统一从 Spring 容器获取，后续如果要补超时、日志或拦截器，
     * 只需要在这里集中调整即可。
     *
     * @return RestTemplate 实例
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
