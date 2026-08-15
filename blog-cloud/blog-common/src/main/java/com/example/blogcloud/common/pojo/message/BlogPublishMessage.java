package com.example.blogcloud.common.pojo.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 博客事件消息(通过 RocketMQ 在服务间传递)
 * <p>
 * 场景: blog-service 删除博客后发送该消息, user-service 消费后把作者的 blog_count 减 1。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPublishMessage {
    /** 博客 id */
    private Integer blogId;
    /** 博客标题 */
    private String title;
    /** 作者(用户) id */
    private Integer userId;
}
