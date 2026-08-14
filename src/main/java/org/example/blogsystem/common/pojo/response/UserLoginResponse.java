package org.example.blogsystem.common.pojo.response;

import lombok.Data;

@Data
public class UserLoginResponse {
    private Integer userId;
    private String token;
    private String userName;

    public UserLoginResponse(Integer id, String token, String userName) {
        this.userId = id;
        this.token = token;
        this.userName = userName;
    }
}
