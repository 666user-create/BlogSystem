package com.example.blogcloud.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Collections;
import java.util.Set;

/**
 * 组件二(Sentinel)网关限流配置
 * <p>
 * 给路由 blog_service(/blog/**)设置 QPS=2 的限流规则:
 * 快速刷新博客列表页面(超过每秒 2 次)即可触发网关限流, 返回自定义提示。
 */
@Slf4j
@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void init() {
        // 路由 id 对应 application.yml 中的 blog_service
        GatewayFlowRule rule = new GatewayFlowRule("blog_service");
        rule.setCount(2);
        rule.setIntervalSec(1);
        GatewayRuleManager.loadRules(Collections.singleton(rule));
        log.info("Sentinel 网关限流规则已加载: 路由 blog_service QPS=2");

        // 自定义被限流时的返回内容(BlockRequestHandler 返回 Mono<ServerResponse>;
        // 注意 WebFlux 6 里 bodyValue() 已经直接返回 Mono, 不要再调 .build())
        GatewayCallbackManager.setBlockHandler((exchange, t) ->
                ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{\"code\":-1,\"errMsg\":\"请求过于频繁, 已被 Sentinel 网关限流\"}"));
    }

    /**
     * 注册 Sentinel 网关过滤器
     */
    @Bean
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }
}
