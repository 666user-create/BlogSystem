package org.example.blogsystem.common.pojo.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class UserRegisterRequest {
    @NotNull(message = "用户名不能为空")
    @Length(min = 4, max = 20, message = "用户名长度必须在4到20之间")
    private String userName;

    /** 明文密码（脚本/接口测试使用） */
    private String password;

    /** 明文确认密码（脚本/接口测试使用） */
    private String confirmPassword;

    /** 前端 RSA 加密后的密码（浏览器使用） */
    private String encryptedPassword;

    /** 前端 RSA 加密后的确认密码（浏览器使用） */
    private String encryptedConfirmPassword;

    private String githubUrl;

    /** 明文与密文至少传一组，否则视为参数校验失败（400） */
    @JsonIgnore
    @AssertTrue(message = "密码不能为空")
    public boolean isPasswordPresent() {
        return (password != null && !password.isEmpty())
                || (encryptedPassword != null && !encryptedPassword.isEmpty());
    }
}
