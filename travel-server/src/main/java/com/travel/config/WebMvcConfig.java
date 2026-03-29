package com.travel.config;

import com.travel.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

/**
 * Web MVC 配置。
 * <p>
 * 负责统一注册跨域规则、鉴权拦截器以及静态资源映射。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 配置全局跨域规则。
     *
     * @param registry Spring MVC 跨域注册器
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 注册接口鉴权拦截器，并排除登录、公开查询和文档等无需鉴权的路径。
     *
     * @param registry Spring MVC 拦截器注册器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(java.util.Objects.requireNonNull(authInterceptor))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/wx-login",
                        "/api/v1/auth/wx-prepare-bind-phone",
                        "/api/v1/auth/wx-bind-phone",
                        "/api/v1/auth/web-prepare-register",
                        "/api/v1/auth/web-register",
                        "/api/v1/auth/web-login",
                        "/api/v1/spots",
                        "/api/v1/spots/search",
                        "/api/v1/spots/filters",
                        "/api/v1/home/**",
                        "/api/v1/guides",
                        "/api/v1/guides/categories",
                        "/api/admin/v1/auth/login",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/uploads/**"
                );
    }

    /**
     * 注册上传目录的静态资源映射。
     *
     * @param registry Spring MVC 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 静态资源映射 - 使用绝对路径
        String uploadPath = System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
