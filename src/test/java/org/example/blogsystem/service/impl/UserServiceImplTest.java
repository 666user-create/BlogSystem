package org.example.blogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.pojo.dataObject.UserInfo;
import org.example.blogsystem.common.pojo.request.UserLoginRequest;
import org.example.blogsystem.common.pojo.request.UserRegisterRequest;
import org.example.blogsystem.common.pojo.response.UserInfoResponse;
import org.example.blogsystem.common.pojo.response.UserLoginResponse;
import org.example.blogsystem.common.utils.SecurityUtil;
import org.example.blogsystem.mapper.UserInfoMapper;
import org.example.blogsystem.service.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl 单元测试
 * ============================================================
 * 用 Mockito 做单元测试的核心思想：
 *   "被测对象"是 UserServiceImpl，它依赖数据库（UserInfoMapper）和其他服务（BlogService）。
 *   单元测试里我们【不连数据库】，而是用 @Mock 造一个"假的 Mapper"，
 *   告诉它"当别人调用 selectOne 时，直接返回我指定的数据"，
 *   然后专心验证 UserServiceImpl 自己的逻辑（分支、异常、参数是否正确传递）。
 *
 * 三个注解：
 *   @ExtendWith(MockitoExtension.class) 开启 Mockito 能力
 *   @Mock       创建一个假对象（代替真实依赖）
 *   @InjectMocks 把假对象注入到 UserServiceImpl 里
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    /** 假的用户表 Mapper（代替真实数据库） */
    @Mock
    private UserInfoMapper userInfoMapper;

    /** 假的博客服务（UserServiceImpl 里统计博客数时会用到） */
    @Mock
    private BlogService blogService;

    /** 被测对象：把上面两个假对象注入进去 */
    @InjectMocks
    private UserServiceImpl userService;

    /** 每个测试方法前构造一个"已注册用户"的数据对象 */
    private UserInfo buildUser(Integer id, String userName, String password) {
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setUserName(userName);
        user.setPassword(password);
        user.setDeleteFlag(0);
        return user;
    }

    /** 构造登录请求对象 */
    private UserLoginRequest buildLoginRequest(String userName, String password) {
        UserLoginRequest request = new UserLoginRequest();
        request.setUserName(userName);
        request.setPassword(password);
        return request;
    }

    /** 构造注册请求对象 */
    private UserRegisterRequest buildRegisterRequest(String userName, String password, String confirm) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUserName(userName);
        request.setPassword(password);
        request.setConfirmPassword(confirm);
        return request;
    }

    // ==================== 登录（checkPassWord） ====================

    @Test
    @DisplayName("登录成功：密码正确返回 token")
    void checkPassWord_success() {
        // 1. 准备：数据库里存了一个 userA，密码是加密后的 123456
        UserInfo user = buildUser(1, "userA", SecurityUtil.encrypt("123456"));
        // 2. 约定假 Mapper：无论用什么条件查询，都返回这个 user
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        // 3. 执行被测方法
        UserLoginResponse result = userService.checkPassWord(buildLoginRequest("userA", "123456"));

        // 4. 断言：返回了用户 id、用户名，且 token 非空
        assertEquals(1, result.getUserId());
        assertEquals("userA", result.getUserName());
        assertNotNull(result.getToken(), "登录成功必须返回 token");
    }

    @Test
    @DisplayName("登录失败：用户不存在抛异常")
    void checkPassWord_userNotExist() {
        // 假 Mapper 返回 null = 数据库里查不到这个用户
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // assertThrows 断言"执行时会抛出指定异常"
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
        BlogException ex = assertThrows(BlogException.class, () -> userService
                .register(buildRegisterRequest("newUser", "123456", "654321")));
        assertEquals("两次输入的密码不一致", ex.getMessage());
        // 校验失败后不应执行任何数据库插入
        verify(userInfoMapper, never()).insert(any(UserInfo.class));
    }

    @Test
    @DisplayName("注册失败：用户名已被占用")
    void register_userNameExists() {
        // selectCount 返回 1 = 用户名已存在
        when(userInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

        BlogException ex = assertThrows(BlogException.class, () -> userService
                .register(buildRegisterRequest("userA", "123456", "123456")));
        assertEquals("用户名已被注册", ex.getMessage());
    }

    @Test
    @DisplayName("注册成功：密码加密存储，插入数据库")
    void register_success() {
        // selectCount 返回 0 = 用户名可用
        when(userInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);

        userService.register(buildRegisterRequest("newUser", "123456", "123456"));

        // ArgumentCaptor 用来"捕获"传给 insert 方法的参数，方便断言内部细节
        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).insert(captor.capture());

        UserInfo saved = captor.getValue();
        assertEquals("newUser", saved.getUserName());
        assertEquals(0, saved.getDeleteFlag());
        // 密码必须是加密的：不等于明文，且长度是 64（32 位盐 + 32 位 MD5）
        assertTrue(!saved.getPassword().equals("123456"));
        assertEquals(64, saved.getPassword().length());
    }

    // ==================== 查询用户信息（getUserInfo） ====================

    @Test
    @DisplayName("查询用户信息成功，返回博客数量")
    void getUserInfo_success() {
        UserInfo user = buildUser(1, "userA", "x");
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);
        when(blogService.countByUserId(1)).thenReturn(3);   // 假博客服务：该用户有 3 篇博客

        UserInfoResponse response = userService.getUserInfo(1);

        assertEquals("userA", response.getUserName());
        assertEquals(3, response.getBlogCount());
    }

    @Test
    @DisplayName("查询不存在的用户返回 null")
    void getUserInfo_notExist() {
        when(userInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        assertNull(userService.getUserInfo(999));
    }

    // ==================== 根据博客查作者（getAuthorInfo） ====================

    @Test
    @DisplayName("根据博客查作者：博客不存在抛异常")
    void getAuthorInfo_blogNotExist() {
        when(blogService.getBlogInfo(999)).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class,
                () -> userService.getAuthorInfo(999));
        assertEquals("博客不存在", ex.getMessage());
    }
}
