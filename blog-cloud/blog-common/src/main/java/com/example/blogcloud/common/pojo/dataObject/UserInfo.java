package com.example.blogcloud.common.pojo.dataObject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("user_info")
public class UserInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("user_name")
    private String userName;
    @TableField("password")
    private String password;
    @TableField("github_url")
    private String githubUrl;
    /** 博客数(冗余字段, 供 Seata / RocketMQ 演示) */
    @TableField("blog_count")
    private Integer blogCount;
    @TableField("delete_flag")
    private Integer deleteFlag;
    @TableField("create_time")
    private LocalDate createTime;
    @TableField("update_time")
    private LocalDate updateTime;
}
