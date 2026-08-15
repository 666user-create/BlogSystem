package com.example.blogcloud.user.client;

import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调用博客服务的 Feign 客户端
 * <p>
 * 组件演示: 服务间远程调用 —— user-service 通过服务名 "blog-service" 调用博客服务,
 * 由 Nacos 完成服务发现、LoadBalancer 完成负载均衡。
 */
@FeignClient(name = "blog-service")
public interface BlogClient {

    /**
     * 获取博客数据对象(内部接口, 不套 Result 壳, 见 ResponseAdvice)
     */
    @GetMapping("/blog/internal/getBlogInfo")
    BlogInfo getBlogInfo(@RequestParam("blogId") Integer blogId);
}
