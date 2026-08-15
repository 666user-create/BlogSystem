package com.example.blogcloud.common.config;

import com.example.blogcloud.common.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>
 * 各服务启动时从 application.yml 读取密钥与过期时间, 注入到 {@link JwtUtils}。
 * 注意: 网关/用户服务/博客服务必须使用同一个密钥。
 */
@Slf4j
@Configuration
public class JwtConfig {

    @Value("${blog.jwt.secret}")
    private String secret;

    @Value("${blog.jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    public void initJwt() {
        JwtUtils.init(secret, expiration);
    }
}
