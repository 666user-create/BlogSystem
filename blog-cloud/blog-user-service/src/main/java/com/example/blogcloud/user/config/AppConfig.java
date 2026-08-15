package com.example.blogcloud.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置中心演示用的业务配置
 * <p>
 * 配合 Nacos: 修改 Nacos 上 user-service.yaml 的 app.switch 后,
 * Spring Cloud 会发布刷新事件并自动重新绑定该 Bean(无需重启)。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    /** 业务开关: 仅用于演示配置热更新(注意 switch 是 Java 关键字, 用 switchOn 代替) */
    private String switchOn;
}
