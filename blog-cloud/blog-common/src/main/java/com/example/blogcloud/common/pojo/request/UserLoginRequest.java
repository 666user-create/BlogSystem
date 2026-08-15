package com.example.blogcloud.common.pojo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserLoginRequest {
    @NotNull(message = "用户名不能为空")
    @Length(min = 4, max = 20, message = "用户名长度必须在4到20之间")
    private String userName;
    @NotNull(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度必须在6到20之间")
    private String password;
}
