package com.example.blogcloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 网关启动类
 * <p>
 * 注意: 只扫描本包(不扫描 blog-common 的 JwtConfig 等 @Configuration),
 * 避免把 Web MVC 相关配置带进响应式网关。
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
