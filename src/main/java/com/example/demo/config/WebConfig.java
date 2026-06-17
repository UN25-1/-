package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置 —— 将上传目录映射为静态资源，通过 /uploads/** 访问图片
 * 当请求的图片不存在时，自动返回默认占位图 img.png，避免前端 500 错误。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        // 确保目录存在
        try {
            java.nio.file.Files.createDirectories(path);
        } catch (Exception e) {
            log.warn("无法创建上传目录: {}", path);
        }

        // 使用 file: 协议映射本地磁盘目录
        String location = "file:" + path.toString().replace("\\", "/") + "/";
        log.info("静态资源映射: /uploads/** -> {}", location);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource original = location.createRelative(resourcePath);
                        if (original.exists() && original.isReadable()) {
                            return original;
                        }
                        // 文件不存在时返回默认占位图
                        Resource fallback = location.createRelative("img.png");
                        if (fallback.exists() && fallback.isReadable()) {
                            log.warn("图片不存在，使用默认占位图: {}", resourcePath);
                            return fallback;
                        }
                        return null;
                    }
                });
    }
}
