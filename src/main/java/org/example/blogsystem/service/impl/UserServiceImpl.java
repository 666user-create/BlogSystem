package org.example.blogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.enums.ResultCodeEnum;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.limiter.LoginAttemptLimiter;
import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.example.blogsystem.common.pojo.dataObject.UserInfo;
import org.example.blogsystem.common.pojo.request.UserLoginRequest;
import org.example.blogsystem.common.pojo.request.UserRegisterRequest;
import org.example.blogsystem.common.pojo.response.UserInfoResponse;
import org.example.blogsystem.common.pojo.response.UserLoginResponse;
import org.example.blogsystem.common.utils.JwtUtils;
import org.example.blogsystem.common.utils.MyBeanUtils;
import org.example.blogsystem.common.utils.SecurityUtil;
import org.example.blogsystem.mapper.UserInfoMapper;
import org.example.blogsystem.service.BlogService;
import org.example.blogsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;
   @Resource(name = "blogServiceImpl")
    private BlogService blogService;
    @Autowired
    private LoginAttemptLimiter loginAttemptLimiter;

    @Override
    public UserLoginResponse checkPassWord(UserLoginRequest userLoginRequest) {
        String userName = userLoginRequest.getUserName();
        // BUG-08：先看是否因连续失败被限流（只统计失败次数，成功会清零，正常用户不受影响）
        long blockedSeconds = loginAttemptLimiter.blockedSeconds(userName);
        if (blockedSeconds > 0) {
            log.warn("用户 {} 登录被限流，剩余 {} 秒", userName, blockedSeconds);
            throw new BlogException(ResultCodeEnum.TOO_MANY_REQUESTS,
                    "登录尝试过于频繁，请 " + blockedSeconds + " 秒后再试");
        }
        QueryWrapper<UserInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(UserInfo::getUserName,userLoginRequest.getUserName())
                .eq(UserInfo::getDeleteFlag,0);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if(userInfo==null){
            loginAttemptLimiter.recordFailure(userName);
            throw new BlogException(ResultCodeEnum.USER_NOT_EXIST, "用户不存在");
        }
        // 注意：此处禁止打印密码或密码哈希（敏感信息泄露风险），仅记录用户名
        if (!SecurityUtil.verify(userLoginRequest.getPassword(), userInfo.getPassword())) {
            loginAttemptLimiter.recordFailure(userName);
            log.warn("用户 {} 密码验证失败", userLoginRequest.getUserName());
            throw new BlogException(ResultCodeEnum.PASSWORD_ERROR, "密码错误");
        }
        // 登录成功：清空失败计数
        loginAttemptLimiter.reset(userName);
        Map<String,Object> map=new HashMap<>();
        map.put("id",userInfo.getId());
        map.put("name",userInfo.getUserName());
        String token= JwtUtils.genJwt(map);
        return new UserLoginResponse(userInfo.getId(),token,userInfo.getUserName());
    }

    @Override
    public void register(UserRegisterRequest request) {
        // 1. 校验两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BlogException(ResultCodeEnum.PARAM_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查用户名是否已被占用（包含已删除的用户）
        QueryWrapper<UserInfo> qw = new QueryWrapper<>();
        qw.lambda().eq(UserInfo::getUserName, request.getUserName());
        if (userInfoMapper.selectCount(qw) > 0) {
            throw new BlogException(ResultCodeEnum.USERNAME_EXISTS, "用户名已被注册");
        }
        // 3. 创建用户
        UserInfo user = new UserInfo();
        user.setUserName(request.getUserName());
        user.setPassword(SecurityUtil.encrypt(request.getPassword()));
        user.setDeleteFlag(0);
        user.setGithubUrl(request.getGithubUrl());
        user.setCreateTime(java.time.LocalDate.now());
        user.setUpdateTime(java.time.LocalDate.now());
        userInfoMapper.insert(user);
        log.info("新用户注册成功: {}", request.getUserName());
    }

    @Override
    public UserInfoResponse getUserInfo(Integer userId) {
        QueryWrapper<UserInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(UserInfo::getId,userId)
                .eq(UserInfo::getDeleteFlag,0);
        UserInfo userInfo=userInfoMapper.selectOne(queryWrapper);
        UserInfoResponse response = MyBeanUtils.transUserInfo(userInfo);
        if (response != null) {
            // 统计该用户的博客数量
            Integer blogCount = blogService.countByUserId(response.getId());
            response.setBlogCount(blogCount);
        }
        return response;
    }

    @Override
    public UserInfoResponse getAuthorInfo(Integer blogId) {
        BlogInfo blogInfo= blogService.getBlogInfo(blogId);
        if(blogInfo==null||blogInfo.getId()==null){
            throw new BlogException(ResultCodeEnum.BLOG_NOT_FOUND, "博客不存在");
        }
        // blogInfo.userId 存的是作者 id
        return getUserInfo(Integer.valueOf(blogInfo.getUserId()));
    }
}
