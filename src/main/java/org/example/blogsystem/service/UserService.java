package org.example.blogsystem.service;

import jakarta.validation.constraints.NotNull;
import org.example.blogsystem.common.pojo.request.UserLoginRequest;
import org.example.blogsystem.common.pojo.request.UserRegisterRequest;
import org.example.blogsystem.common.pojo.response.Result;
import org.example.blogsystem.common.pojo.response.UserInfoResponse;
import org.example.blogsystem.common.pojo.response.UserLoginResponse;

public interface UserService {
    UserLoginResponse checkPassWord(UserLoginRequest userLoginRequest);

    void register(UserRegisterRequest request);

    UserInfoResponse getUserInfo(@NotNull(message = "用户id不能为空") Integer userId);

    UserInfoResponse getAuthorInfo(@NotNull(message = "博客id不能为空") Integer blogId);
}
