package com.example.blogcloud.blog.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.blogcloud.blog.client.UserClient;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 作者信息服务
 * <p>
 * 组件二(Sentinel)演示: 通过 Feign 调用户服务拿作者名,
 * 用 @SentinelResource 定义资源 "getAuthorName" —— 用户服务挂了时,
 * 调用异常被 Sentinel 统计, 异常数达到阈值后触发熔断, 走 fallback 返回"未知作者"。
 */
@Slf4j
@Service
public class AuthorInfoService {

    @Autowired
    private UserClient userClient;

    /**
     * 获取作者名
     */
    @SentinelResource(value = "getAuthorName", fallback = "getAuthorNameFallback")
    public String getAuthorName(String userId) {
        Integer uid;
        try {
            uid = Integer.valueOf(userId);
        } catch (NumberFormatException e) {
            return "未知作者";
        }
        UserInfoResponse user = userClient.getUserInfo(uid);
        return user == null ? "未知作者" : user.getUserName();
    }

    /**
     * 兜底方法: 参数与业务方法一致, 末尾多一个 Throwable
     */
    public String getAuthorNameFallback(String userId, Throwable throwable) {
        log.warn("获取作者名失败, 触发 Sentinel 兜底: {}", throwable.getMessage());
        return "未知作者";
    }
}
