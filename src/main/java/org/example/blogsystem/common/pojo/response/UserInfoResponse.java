package org.example.blogsystem.common.pojo.response;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;
@Data
public class UserInfoResponse {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String userName;
    private String githubUrl;
    /**
     * 该用户的博客数量
     */
    private Integer blogCount;
}
