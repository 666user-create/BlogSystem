package org.example.blogsystem.common.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddBlogRequest {
    @NotNull(message = "用户 id 不能为空")
    private Integer userId;
    @NotNull(message = "标题不能为空")
    private String title;
    @NotNull(message = "内容不能为空")
    private String content;
}
