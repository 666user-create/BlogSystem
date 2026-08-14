package org.example.blogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.exception.BlogException;
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
    @Override
    public UserLoginResponse checkPassWord(UserLoginRequest userLoginRequest) {
        QueryWrapper<UserInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(UserInfo::getUserName,userLoginRequest.getUserName())
                .eq(UserInfo::getDeleteFlag,0);
        UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);
        if(userInfo==null){
            throw new BlogException("用户不存在");
        }
        log.info("数据库存储的密码: length={}, 完整值=[{}]",
            userInfo.getPassword() != null ? userInfo.getPassword().length() : 0,
            userInfo.getPassword());
        if (!SecurityUtil.verify(userLoginRequest.getPassword(), userInfo.getPassword())) {
            log.warn("密码验证失败: 输入=[{}], 数据库值=[{}]", userLoginRequest.getPassword(), userInfo.getPassword());
            throw new BlogException("密码错误");
        }
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
            throw new BlogException("两次输入的密码不一致");
        }
        // 2. 检查用户名是否已被占用（包含已删除的用户）
        QueryWrapper<UserInfo> qw = new QueryWrapper<>();
        qw.lambda().eq(UserInfo::getUserName, request.getUserName());
        if (userInfoMapper.selectCount(qw) > 0) {
            throw new BlogException("用户名已被注册");
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
            throw new BlogException("博客不存在");
        }
        // blogInfo.userId 存的是作者 id
        return getUserInfo(Integer.valueOf(blogInfo.getUserId()));
    }
}
