package com.example.blogcloud.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 博客服务启动类
 * <p>
 * 注意: @MapperScan 只扫 mapper 子包 —— 若扫整个 com.example.blogcloud,
 * BlogService 等业务接口也会被注册成 Mapper 代理, 导致注入错 bean。
 */
@SpringBootApplication(scanBasePackages = "com.example.blogcloud")
@MapperScan("com.example.blogcloud.blog.mapper")
@EnableFeignClients(basePackages = "com.example.blogcloud.blog.client")
public class BlogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }
}
