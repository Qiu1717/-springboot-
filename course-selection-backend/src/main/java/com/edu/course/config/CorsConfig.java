package com.edu.course.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置 + 静态资源映射
 * 
 * CORS: 前端在localhost:3000，后端在localhost:8080 → 不同"源"(origin)，需要CORS放行
 * 静态资源: 上传的文件存储在本地磁盘，通过 /uploads/** URL直接访问
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")  // E:/springboot课设项目/uploads
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 所有接口都允许跨域
                .allowedOriginPatterns("*")  // 允许任意来源(生产环境应限制为具体域名)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")  // 允许所有请求头(包括Authorization)
                .allowCredentials(true)  // 允许携带Cookie(配合allowedOriginPatterns而非allowedOrigins)
                .maxAge(3600);  // 预检请求(OPTIONS)的缓存时间(秒)
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/xxx 映射到本地磁盘 E:/springboot课设项目/uploads/xxx
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");  // "file:"前缀表示本地文件系统
    }
}
