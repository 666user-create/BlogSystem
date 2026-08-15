package com.example.blogcloud.blog.client;

import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调用用户服务的 Feign 客户端
 * <p>
 * 组件演示: 服务间远程调用 —— blog-service 通过服务名 "user-service" 调用用户服务。
 */
@FeignClient(name = "user-service")
public interface UserClient {

    /** 查询用户信息(用于博客详情展示作者名) */
    @GetMapping("/user/getUserInfo")
    UserInfoResponse getUserInfo(@RequestParam("userId") Integer userId);

    /** Seata 全局事务分支: 博客数 +1(内部接口, 不套 Result 壳) */
    @PostMapping("/user/internal/increaseBlogCount")
    Boolean increaseBlogCount(@RequestParam("userId") Integer userId);
}
