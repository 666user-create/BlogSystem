package com.example.blogcloud.user.controller;

import com.example.blogcloud.common.pojo.request.UserLoginRequest;
import com.example.blogcloud.common.pojo.request.UserRegisterRequest;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import com.example.blogcloud.common.pojo.response.UserLoginResponse;
import com.example.blogcloud.user.config.AppConfig;
import com.example.blogcloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @Autowired
    private AppConfig appConfig;

    /** 用户登录 */
    @PostMapping("/login")
    public UserLoginResponse login(@RequestBody @Validated UserLoginRequest userLoginRequest) {
        log.info("用户登录, 用户名: {}", userLoginRequest.getUserName());
        return userService.checkPassWord(userLoginRequest);
    }

    /** 用户注册 */
    @PostMapping("/register")
    public String register(@RequestBody @Validated UserRegisterRequest request) {
        log.info("用户注册, 用户名: {}", request.getUserName());
        userService.register(request);
        return "注册成功";
    }

    /** 根据用户 id 获取用户信息 */
    @GetMapping("/getUserInfo")
    public UserInfoResponse getUserInfo(@RequestParam @NotNull(message = "用户id不能为空") Integer userId) {
        log.info("获取用户信息, userId: {}", userId);
        return userService.getUserInfo(userId);
    }

    /** 根据博客 id 获取作者信息(内部通过 Feign 调博客服务) */
    @GetMapping("/getAuthorInfo")
    public UserInfoResponse getAuthorInfo(@RequestParam @NotNull(message = "博客id不能为空") Integer blogId) {
        log.info("根据博客 id 获取作者信息, blogId: {}", blogId);
        return userService.getAuthorInfo(blogId);
    }

    /** 配置中心演示: 返回当前 app.switch-on 的值, 验证 Nacos 配置热更新 */
    @GetMapping("/configInfo")
    public String configInfo() {
        return "app.switch-on = " + appConfig.getSwitchOn();
    }

    /**
     * Seata 全局事务分支: 博客数 +1(仅服务间 Feign 调用, 内部接口不套 Result 壳)
     */
    @PostMapping("/internal/increaseBlogCount")
    public Boolean increaseBlogCount(@RequestParam @NotNull(message = "用户id不能为空") Integer userId) {
        log.info("Seata 分支接口被调用: userId = {}", userId);
        return userService.increaseBlogCount(userId);
    }
}
