package com.example.blogcloud.blog.service;

import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.response.BlogInfoResponse;

import java.util.List;

public interface BlogService {
    List<BlogInfoResponse> getList();

    BlogInfoResponse getBlogDetail(Integer blogId);

    BlogInfo getBlogInfo(Integer blogId);

    /**
     * 新增博客(Seata 全局事务入口: 写博客 + 远程增加用户博客数)
     */
    void addBlog(String title, String content, Integer userId);

    void updateBlog(Integer blogId, String title, String content, Integer userId);

    /**
     * 删除博客(逻辑删除; RocketMQ 演示: 删除后发送博客事件消息)
     */
    void deleteBlog(Integer blogId, Integer userId);

    List<BlogInfoResponse> adminList();

    void togglePublish(Integer blogId, Integer adminUserId);
}
