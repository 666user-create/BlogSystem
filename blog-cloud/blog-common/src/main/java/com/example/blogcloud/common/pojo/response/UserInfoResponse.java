package com.example.blogcloud.common.pojo.response;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Integer id;
    private String userName;
    private String githubUrl;
    /** 该用户的博客数量 */
    private Integer blogCount;
}
