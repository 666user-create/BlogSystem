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
import com.example.blogcloud.common.utils.SecurityUtil;
import com.example.blogcloud.user.client.BlogClient;
import com.example.blogcloud.user.mapper.UserInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl 单元测试（微服务版）
 * ============================================================
 * 与单体工程同一套 Mockito 套路，差异点：
 *   1. 博客数据通过 Feign（BlogClient）远程获取 —— 用 @Mock 替代远程调用；
 *   2. 博客数不再是跨服务统计，而是读 user_info.blog_count 冗余字段；
 *   3. 新增 Seata 分支方法 increaseBlogCount（更新 blog_count）。
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /** 假的用户表 Mapper */
    @Mock
    private UserInfoMapper userInfoMapper;

    /** 假的 Feign 客户端（代替对博客服务的远程调用） */
    @Mock
    private BlogClient blogClient;

    /** 被测对象 */
    @InjectMocks
    private UserServiceImpl userService;

    private UserInfo buildUser(Integer id, String userName, String password) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUserName(userName);
        user.setPassword(password);
        user.setDeleteFlag(0);
        user.setBlogCount(3);   // 微服务版冗余字段
        return user;
    }

    private UserLoginRequest buildLoginRequest(String userName, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    // ==================== 登录（checkPassWord） ====================

    @Test
    @DisplayName("登录成功：密码正确返回 token")
    void checkPassWord_success() {
        UserInfo user = buildUser(1, "userA", SecurityUtil.encrypt("123456"));
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        UserLoginResponse result = userService.checkPassWord(buildLoginRequest("userA", "123456"));

        assertEquals(1, result.getUserId());
        assertEquals("userA", result.getUserName());
        assertNotNull(result.getToken(), "登录成功必须返回 token");
    }

    @Test
    @DisplayName("登录失败：用户不存在抛异常")
    void checkPassWord_userNotExist() {
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class,
                () -> userService.checkPassWord(buildLoginRequest("ghost", "123456")));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登录失败：密码错误抛异常")
    void checkPassWord_wrongPassword() {
        UserInfo user = buildUser(1, "userA", SecurityUtil.encrypt("123456"));
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        BlogException ex = assertThrows(BlogException.class,
                () -> userService.checkPassWord(buildLoginRequest("userA", "wrong")));
        assertEquals("密码错误", ex.getMessage());
    }

    // ==================== 注册（register） ====================

    @Test
    @DisplayName("注册失败：两次密码不一致")
    void register_passwordMismatch() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserName("newUser");
        request.setPassword("123456");
        request.setConfirmPassword("654321");

        BlogException ex = assertThrows(BlogException.class, () -> userService.register(request));
        assertEquals("两次输入的密码不一致", ex.getMessage());
        verify(userInfoMapper, never()).insert(any(UserInfo.class));
    }

    @Test
    @DisplayName("注册失败：用户名已被占用")
    void register_userNameExists() {
        when(userInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserName("userA");
        request.setPassword("123456");
        request.setConfirmPassword("123456");

        BlogException ex = assertThrows(BlogException.class, () -> userService.register(request));
        assertEquals("用户名已被注册", ex.getMessage());
    }

    @Test
    @DisplayName("注册成功：密码加密、blog_count 初始为 0")
    void register_success() {
        when(userInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserName("newUser");
        request.setPassword("123456");
        request.setConfirmPassword("123456");
        userService.register(request);

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).insert(captor.capture());

        UserInfo saved = captor.getValue();
        assertEquals("newUser", saved.getUserName());
        assertEquals(0, saved.getBlogCount(), "新用户博客数初始为 0");
        assertEquals(64, saved.getPassword().length(), "密码必须是 32 位盐 + 32 位 MD5");
    }

    // ==================== 查询用户信息（getUserInfo） ====================

    @Test
    @DisplayName("查询用户信息成功：直接读冗余字段 blog_count（无跨服务调用）")
    void getUserInfo_success() {
        UserInfo user = buildUser(1, "userA", "x");   // blogCount = 3
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        UserInfoResponse response = userService.getUserInfo(1);

        assertEquals("userA", response.getUserName());
        assertEquals(3, response.getBlogCount(), "博客数来自冗余字段");
    }

    @Test
    @DisplayName("查询不存在的用户返回 null")
    void getUserInfo_notExist() {
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        assertNull(userService.getUserInfo(999));
    }

    // ==================== 根据博客查作者（getAuthorInfo，走 Feign） ====================

    @Test
    @DisplayName("根据博客查作者：Feign 返回空博客时抛异常")
    void getAuthorInfo_blogNotExist() {
        when(blogClient.getBlogInfo(999)).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class, () -> userService.getAuthorInfo(999));
        assertEquals("博客不存在", ex.getMessage());
    }

    @Test
    @DisplayName("根据博客查作者：Feign 拿到博客后返回作者信息")
    void getAuthorInfo_success() {
        // 博客服务返回的博客（userId=1）
        BlogInfo blogInfo = new BlogInfo();
        blogInfo.setId(100);
        blogInfo.setUserId("1");
        when(blogClient.getBlogInfo(100)).thenReturn(blogInfo);

        // 再查用户表拿到作者信息
        UserInfo author = buildUser(1, "作者A", "x");
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(author);

        UserInfoResponse response = userService.getAuthorInfo(100);

        assertEquals("作者A", response.getUserName());
        verify(blogClient).getBlogInfo(100);   // 确认走了 Feign 远程调用
    }

    // ==================== Seata 分支（increaseBlogCount） ====================

    @Test
    @DisplayName("Seata 分支：博客数 +1 成功")
    void increaseBlogCount_success() {
        when(userInfoMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);

        assertTrue(userService.increaseBlogCount(1));
        verify(userInfoMapper).update(any(), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("Seata 分支：影响行数为 0 返回 false")
    void increaseBlogCount_fail() {
        when(userInfoMapper.update(any(), any(UpdateWrapper.class))).thenReturn(0);

        assertFalse(userService.increaseBlogCount(1));
    }
}
