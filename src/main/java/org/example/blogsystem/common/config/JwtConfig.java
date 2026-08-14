package org.example.blogsystem.common.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 * <p>
 * 应用启动时从配置文件（application.yml）读取密钥与过期时间，
 * 注入到 {@link JwtUtils}，避免密钥硬编码在源码中。
 * 生产环境可通过环境变量 BLOG_JWT_SECRET 覆盖。
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
