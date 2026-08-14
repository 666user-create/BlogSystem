package org.example.blogsystem.common.pojo.response;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogInfoResponse {
    //定义返回给前端的博客信息响应类
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
    private String userId;
    private Integer publishedStatus;
    @JsonFormat(pattern = "yyyy年MM月dd日 HH:mm:ss")
    //格式化时间,也可使用DateUtils.formatLocalDateTime()方法格式化
    private LocalDateTime createTime;

    /**
     * 列表摘要：仅用于列表页展示的简短内容
     * 返回内容前 50 个字符（不足 50 则全部返回）
     */
    public String getSummary() {
        if (content == null) {
            return "";
        }
        return content.length() <= 50 ? content : content.substring(0, 50);
    }
}
