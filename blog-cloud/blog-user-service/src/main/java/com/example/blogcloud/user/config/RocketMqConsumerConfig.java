package com.example.blogcloud.user.config;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.blogcloud.common.pojo.dataObject.UserInfo;
import com.example.blogcloud.common.pojo.message.BlogPublishMessage;
import com.example.blogcloud.user.mapper.UserInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.function.Consumer;

/**
 * RocketMQ 消费者配置
 * <p>
 * 组件四(RocketMQ)演示: 消费博客服务发来的"博客删除"事件, 把作者 blog_count - 1。
 * <p>
 * 关键点: 用 @Profile("mq") 控制 —— 只有激活 mq profile 才创建 Consumer Bean,
 * 否则 Spring Cloud Stream 不会创建消费连接, 未启动 RocketMQ 时服务也能正常启动。
 * 启动方式: --spring.profiles.active=mq
 */
@Slf4j
@Configuration
@Profile("mq")
public class RocketMqConsumerConfig {

    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 函数式消费者: Bean 名 blogIn 对应 binding blog-in-0(见 application-mq.yml)
     */
    @Bean
    public Consumer<BlogPublishMessage> blogIn() {
        return message -> {
            log.info("【RocketMQ】消费博客事件: blogId={}, title={}, userId={}",
                    message.getBlogId(), message.getTitle(), message.getUserId());
            UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", message.getUserId())
                    .setSql("blog_count = IF(blog_count > 0, blog_count - 1, 0)");
            int rows = userInfoMapper.update(null, updateWrapper);
            log.info("【RocketMQ】用户 {} 博客数 -1, 影响行数 {}", message.getUserId(), rows);
        };
    }
}
