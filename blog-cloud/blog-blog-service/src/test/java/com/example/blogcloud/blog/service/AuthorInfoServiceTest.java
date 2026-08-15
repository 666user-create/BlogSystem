package com.example.blogcloud.blog.service;

import com.example.blogcloud.blog.client.UserClient;
import com.example.blogcloud.common.pojo.response.UserInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthorInfoService 单元测试
 * ============================================================
 * 演示 Sentinel 熔断兜底逻辑（组件二）：
 *   正常时 Feign 调用户服务拿作者名；
 *   用户服务不可用/返回空时，走 fallback 返回"未知作者"，保证主流程不挂。
 * 注：@SentinelResource 注解在单测中不生效（无 Sentinel 切面），直接验证方法逻辑。
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class AuthorInfoServiceTest {

    @Mock
    private UserClient userClient;

    @InjectMocks
    private AuthorInfoService authorInfoService;

    @Test
    @DisplayName("正常获取作者名：Feign 返回用户信息")
    void getAuthorName_success() {
        UserInfoResponse user = new UserInfoResponse();
        user.setUserName("作者A");
        when(userClient.getUserInfo(1)).thenReturn(user);

        assertEquals("作者A", authorInfoService.getAuthorName("1"));
        verify(userClient).getUserInfo(1);
    }

    @Test
    @DisplayName("userId 非法（非数字）：直接返回未知作者，不发 Feign 调用")
    void getAuthorName_invalidUserId() {
        assertEquals("未知作者", authorInfoService.getAuthorName("abc"));
        verify(userClient, never()).getUserInfo(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("用户服务返回 null：返回未知作者（熔断兜底场景）")
    void getAuthorName_userNotFound() {
        when(userClient.getUserInfo(999)).thenReturn(null);

        assertEquals("未知作者", authorInfoService.getAuthorName("999"));
    }

    @Test
    @DisplayName("fallback 方法：异常时返回未知作者")
    void fallback_method() {
        assertEquals("未知作者",
                authorInfoService.getAuthorNameFallback("1", new RuntimeException("用户服务挂了")));
    }
}
