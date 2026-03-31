package com.travel.config.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 配置。
 * <p>
 * 负责定义接口文档基础信息、JWT 鉴权方案以及用户端/管理端接口分组。
 */
@Configuration
public class SwaggerConfig {

    /**
     * 构建全局 OpenAPI 文档对象。
     *
     * @return OpenAPI 文档配置
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WayTrip·微旅 API")
                        .description("基于协同过滤的个性化旅游推荐系统接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("Travel Team")))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }

    /**
     * 用户端 API 分组。
     *
     * @return 用户端接口分组配置
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/admin/**")
                .build();
    }

    /**
     * 管理端 API 分组。
     *
     * @return 管理端接口分组配置
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .pathsToMatch("/api/admin/**")
                .build();
    }
}
