package com.example.blogcloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.blogcloud.common.exception.BlogException;
import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.dataObject.UserInfo;
import com.example.blogcloud.common.pojo.request.UserLoginRequest;
import com.example.blogcloud.common.pojo.request.UserRegisterRequest;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import com.example.blogcloud.common.pojo.response.UserLoginResponse;
import com.example.blogcloud.common.utils.JwtUtils;
import com.example.blogcloud.common.utils.MyBeanUtils;
import com.example.blogcloud.common.utils.SecurityUtil;
import com.example.blogcloud.user.client.BlogClient;
import com.example.blogcloud.user.mapper.UserInfoMapper;
import com.example.blogcloud.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private BlogClient blogClient;

    @Override
    public UserLoginResponse checkPassWord(UserLoginRequest userLoginRequest) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserInfo::getUserName, userLoginRequest.getUserName())
                .eq(UserInfo::getDeleteFlag, 0);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if (userInfo == null) {
            throw new BlogException("用户不存在");
        }
        if (!SecurityUtil.verify(userLoginRequest.getPassword(), userInfo.getPassword())) {
            log.warn("用户 {} 密码验证失败", userLoginRequest.getUserName());
            throw new BlogException("密码错误");
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", userInfo.getId());
        map.put("name", userInfo.getUserName());
        String token = JwtUtils.genJwt(map);
        return new UserLoginResponse(userInfo.getId(), token, userInfo.getUserName());
    }

    @Override
    public void register(UserRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BlogException("两次输入的密码不一致");
        }
        QueryWrapper<UserInfo> qw = new QueryWrapper<>();
        qw.lambda().eq(UserInfo::getUserName, request.getUserName());
        if (userInfoMapper.selectCount(qw) > 0) {
            throw new BlogException("用户名已被注册");
        }
        UserInfo user = new UserInfo();
        user.setUserName(request.getUserName());
        user.setPassword(SecurityUtil.encrypt(request.getPassword()));
        user.setDeleteFlag(0);
        user.setBlogCount(0);
        user.setGithubUrl(request.getGithubUrl());
        user.setCreateTime(LocalDate.now());
        user.setUpdateTime(LocalDate.now());
        userInfoMapper.insert(user);
        log.info("新用户注册成功: {}", request.getUserName());
    }

    @Override
    public UserInfoResponse getUserInfo(Integer userId) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserInfo::getId, userId)
                .eq(UserInfo::getDeleteFlag, 0);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        UserInfoResponse response = MyBeanUtils.transUserInfo(userInfo);
        if (response != null && userInfo != null) {
            // 微服务版: 博客数直接取 user_info.blog_count 冗余字段
            // (由 Seata 同步 +1 / RocketMQ 异步 -1 维护)
            response.setBlogCount(userInfo.getBlogCount());
        }
        return response;
    }

    @Override
    public UserInfoResponse getAuthorInfo(Integer blogId) {
        // 博客数据属于博客服务, 通过 Feign 远程获取
        BlogInfo blogInfo = blogClient.getBlogInfo(blogId);
        if (blogInfo == null || blogInfo.getId() == null) {
            throw new BlogException("博客不存在");
        }
        return getUserInfo(Integer.valueOf(blogInfo.getUserId()));
    }

    @Override
    public boolean increaseBlogCount(Integer userId) {
        // Seata 全局事务的分支: 博客服务发博客成功后远程调用本方法
        UpdateWrapper<UserInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId).setSql("blog_count = blog_count + 1");
        int rows = userInfoMapper.update(null, updateWrapper);
        log.info("Seata 分支: 用户 {} 博客数 +1, 影响行数 {}", userId, rows);
        return rows > 0;
    }
}
