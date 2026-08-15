package com.example.blogcloud.blog.config;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sentinel 规则配置(代码方式加载, 简单直观; 生产环境一般用控制台动态推送)
 * <p>
 * 组件二(Sentinel)演示:
 * 1. 限流: GET:/blog/getList 每秒最多 1 次, 快速刷新页面即可看到被限流;
 * 2. 熔断: getAuthorName 资源 1 秒内异常数达到 3 次, 熔断 5 秒, 期间直接走兜底。
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @PostConstruct
    public void init() {
        // ===== 1. 接口限流 =====
        FlowRule flowRule = new FlowRule();
        flowRule.setResource("GET:/blog/getList");
        flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        flowRule.setCount(1);
        FlowRuleManager.loadRules(List.of(flowRule));
        log.info("Sentinel 限流规则已加载: GET:/blog/getList QPS=1");

        // ===== 2. 熔断降级 =====
        DegradeRule degradeRule = new DegradeRule("getAuthorName");
        degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);
        degradeRule.setCount(3);
        degradeRule.setTimeWindow(5);
        DegradeRuleManager.loadRules(List.of(degradeRule));
        log.info("Sentinel 熔断规则已加载: getAuthorName 异常数>=3 熔断5秒");
    }
}
