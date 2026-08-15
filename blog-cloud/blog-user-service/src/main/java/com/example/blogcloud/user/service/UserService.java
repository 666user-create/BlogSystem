package com.example.blogcloud.user.service;

import com.example.blogcloud.common.pojo.request.UserLoginRequest;
import com.example.blogcloud.common.pojo.request.UserRegisterRequest;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import com.example.blogcloud.common.pojo.response.UserLoginResponse;

public interface UserService {
    UserLoginResponse checkPassWord(UserLoginRequest userLoginRequest);

    void register(UserRegisterRequest request);

    UserInfoResponse getUserInfo(Integer userId);

    /** 根据博客 id 查作者(通过 Feign 调用博客服务) */
    UserInfoResponse getAuthorInfo(Integer blogId);

    /** 博客数 +1(Seata 分布式事务的分支, 由博客服务 Feign 调用) */
    boolean increaseBlogCount(Integer userId);
}
