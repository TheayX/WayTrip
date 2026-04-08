package com.travel.config.web;

import com.travel.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import java.nio.file.Path;

/**
 * Web MVC 配置。
 * <p>
 * 负责统一注册跨域规则、鉴权拦截器以及静态资源映射。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

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
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/uploads/**",
                        "/api/v1/ai/chat"
                        );
    }

    /**
     * 注册上传目录的静态资源映射。
     *
     * @param registry Spring MVC 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 统一复用上传落盘目录配置，避免“文件已上传但静态访问路径仍指向旧目录”的部署问题。
        String normalizedUploadPath = Path.of(uploadPath)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(normalizedUploadPath);
    }
}
