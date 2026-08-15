package com.example.blogcloud.gateway.config;

import com.example.blogcloud.common.utils.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 网关本地 JWT 初始化(不能依赖 blog-common 里的 JwtConfig, 因为网关不扫描 common 包)
 */
@Slf4j
@Configuration
public class GatewayJwtConfig {

    @Value("${blog.jwt.secret}")
    private String secret;

    @Value("${blog.jwt.expiration:86400000}")
    private long expiration;

    @PostConstruct
    public void initJwt() {
        JwtUtils.init(secret, expiration);
    }
}
