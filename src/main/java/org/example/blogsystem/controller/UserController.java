package org.example.blogsystem.controller;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.pojo.request.UserLoginRequest;
import org.example.blogsystem.common.pojo.request.UserRegisterRequest;
import org.example.blogsystem.common.pojo.response.UserInfoResponse;
import org.example.blogsystem.common.pojo.response.UserLoginResponse;
import org.example.blogsystem.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口
 */
@Slf4j
@RequestMapping("/user")
@RestController
public class UserController {
    @Resource(name = "userServiceImpl")
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public UserLoginResponse login(@RequestBody @Validated UserLoginRequest userLoginRequest) {
        log.info("用户登录, 用户名: {}", userLoginRequest.getUserName());
        return userService.checkPassWord(userLoginRequest);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public String register(@RequestBody @Validated UserRegisterRequest request) {
        log.info("用户注册, 用户名: {}", request.getUserName());
        userService.register(request);
        return "注册成功";
    }

    /**
     * 根据用户 id 获取用户信息
     */
    @GetMapping("/getUserInfo")
    public UserInfoResponse getUserInfo(@NotNull(message = "用户id不能为空") Integer userId) {
        log.info("获取用户信息, userId: {}", userId);
        return userService.getUserInfo(userId);
    }

    /**
     * 根据博客 id 获取作者信息
     */
    @GetMapping("/getAuthorInfo")
    public UserInfoResponse getAuthorInfo(@NotNull(message = "博客id不能为空") Integer blogId) {
        log.info("根据博客 id 获取作者信息, blogId: {}", blogId);
        return userService.getAuthorInfo(blogId);
    }
}
