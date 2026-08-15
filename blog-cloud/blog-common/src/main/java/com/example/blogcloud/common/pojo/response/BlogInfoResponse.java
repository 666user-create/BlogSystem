package com.example.blogcloud.common.pojo.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogInfoResponse {
    private Integer id;
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private String userId;
    /** 作者名: 由 blog-service 通过 Feign 调用 user-service 填充(微服务版新增) */
    private String authorName;
    private Integer publishedStatus;
    @JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss")
    private LocalDateTime createTime;

    /** 列表摘要: 内容前 50 个字符 */
    public String getSummary() {
        if (content == null) {
            return "";
        }
        return content.length() <= 50 ? content : content.substring(0, 50);
    }
}
