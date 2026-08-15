package com.example.blogcloud.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blogcloud.blog.client.UserClient;
import com.example.blogcloud.blog.mapper.BlogInfoMapper;
import com.example.blogcloud.blog.service.AuthorInfoService;
import com.example.blogcloud.common.exception.BlogException;
import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.response.BlogInfoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BlogServiceImpl 单元测试（微服务版）
 * ============================================================
 * 与单体工程同一套 Mockito 套路，差异点（微服务新增逻辑）：
 *   1. addBlog：Seata 全局事务入口，内部 Feign 调用户服务增加博客数（mock UserClient）；
 *   2. getBlogDetail：作者名跨服务获取（mock AuthorInfoService，Sentinel 兜底）；
 *   3. deleteBlog：可发 RocketMQ 删除事件（StreamBridge，mqEnabled 开关）。
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    @Mock
    private BlogInfoMapper blogInfoMapper;

    /** 假的 Feign 客户端（代替对用户服务的远程调用） */
    @Mock
    private UserClient userClient;

    /** 假的作者信息服务（Sentinel 兜底逻辑单独测，见 AuthorInfoServiceTest） */
    @Mock
    private AuthorInfoService authorInfoService;

    /** 假的 RocketMQ 发送器（Spring Cloud Stream） */
    @Mock
    private StreamBridge streamBridge;

    @InjectMocks
    private BlogServiceImpl blogService;

    private BlogInfo buildBlog(Integer id, String userId) {
        BlogInfo blog = new BlogInfo();
        blog.setId(id);
        blog.setTitle("标题");
        blog.setContent("内容");
        blog.setUserId(userId);
        blog.setPublishedStatus(1);
        blog.setDeleteFlag(0);
        blog.setCreateTime(LocalDateTime.now());
        blog.setUpdateTime(LocalDateTime.now());
        return blog;
    }

    // ==================== 新增博客（addBlog，Seata 入口） ====================

    @Test
    @DisplayName("新增失败：标题为空")
    void addBlog_emptyTitle() {
        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.addBlog("   ", "内容", 1));
        assertEquals("标题不能为空", ex.getMessage());
        verify(blogInfoMapper, never()).insert(any(BlogInfo.class));
    }

    @Test
    @DisplayName("新增失败：内容为空")
    void addBlog_emptyContent() {
        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.addBlog("标题", null, 1));
        assertEquals("内容不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("新增成功：写博客 + Feign 调用户服务博客数+1（Seata 分支）")
    void addBlog_success_withSeataBranch() {
        // 远程调用返回成功
        when(userClient.increaseBlogCount(1)).thenReturn(true);

        blogService.addBlog("  新博客  ", "第一篇内容", 1);

        // 1. 博客已插入，字段符合业务规则
        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).insert(captor.capture());
        BlogInfo saved = captor.getValue();
        assertEquals("新博客", saved.getTitle());        // 标题 trim
        assertEquals("1", saved.getUserId());
        assertEquals(1, saved.getPublishedStatus());      // 默认上架
        assertEquals(0, saved.getDeleteFlag());

        // 2. Seata 分支：远程调用用户服务执行了
        verify(userClient).increaseBlogCount(1);
    }

    // ==================== 详情（getBlogDetail，跨服务作者名） ====================

    @Test
    @DisplayName("详情成功：作者名通过 AuthorInfoService 填充")
    void getBlogDetail_success() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));
        when(authorInfoService.getAuthorName("1")).thenReturn("作者A");

        BlogInfoResponse response = blogService.getBlogDetail(1);

        assertEquals("作者A", response.getAuthorName());
        verify(authorInfoService).getAuthorName("1");
    }

    @Test
    @DisplayName("详情失败：博客不存在抛异常")
    void getBlogDetail_notExist() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.getBlogDetail(999));
        assertEquals("博客不存在", ex.getMessage());
    }

    // ==================== 更新博客（updateBlog） ====================

    @Test
    @DisplayName("更新失败：非作者更新他人博客")
    void updateBlog_notOwner() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.updateBlog(1, "新标题", "新内容", 2));
        assertEquals("无权编辑该博客", ex.getMessage());
    }

    @Test
    @DisplayName("更新成功：作者更新自己的博客")
    void updateBlog_owner_success() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));

        blogService.updateBlog(1, "新标题", "新内容", 1);

        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
    }

    // ==================== 删除博客（deleteBlog，RocketMQ 分支） ====================

    @Test
    @DisplayName("删除失败：非作者删除他人博客")
    void deleteBlog_notOwner() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.deleteBlog(1, 2));
        assertEquals("无权删除该博客", ex.getMessage());
    }

    @Test
    @DisplayName("删除成功：逻辑删除；mq 未开启时不发消息")
    void deleteBlog_success_mqDisabled() {
        // 默认 @Value("${app.mq.enabled:false}") = false
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));

        blogService.deleteBlog(1, 1);

        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleteFlag(), "逻辑删除标记应为 1");

        // mq 未开启：不应发 RocketMQ 消息
        verify(streamBridge, never()).send(any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("删除成功且 mq 开启：发送 RocketMQ 删除事件")
    void deleteBlog_success_mqEnabled() {
        // 用 ReflectionTestUtils 把 @Value 字段注入为 true（模拟激活 mq profile）
        ReflectionTestUtils.setField(blogService, "mqEnabled", true);
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(buildBlog(1, "1"));
        when(streamBridge.send(any(String.class), any(Object.class))).thenReturn(true);

        blogService.deleteBlog(1, 1);

        verify(streamBridge).send(eq("blog-out-0"), any(Object.class));
    }

    // ==================== 列表 / 管理端 / 上下架 ====================

    @Test
    @DisplayName("列表查询：返回数据被正确转换")
    void getList_success() {
        when(blogInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(buildBlog(1, "1"), buildBlog(2, "1")));

        List<BlogInfoResponse> list = blogService.getList();

        assertEquals(2, list.size());
        assertEquals(1, list.get(0).getId());
    }

    @Test
    @DisplayName("列表查询：无数据返回空列表")
    void getList_empty() {
        when(blogInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());
        assertEquals(0, blogService.getList().size());
    }

    @Test
    @DisplayName("管理端列表：透传 mapper 结果")
    void adminList_success() {
        when(blogInfoMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.singletonList(buildBlog(1, "1")));
        assertEquals(1, blogService.adminList().size());
    }

    @Test
    @DisplayName("上下架失败：已删除的博客不能操作")
    void togglePublish_deletedBlog() {
        BlogInfo deleted = buildBlog(1, "1");
        deleted.setDeleteFlag(1);
        when(blogInfoMapper.selectById(1)).thenReturn(deleted);

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.togglePublish(1, 1));
        assertEquals("博客不存在", ex.getMessage());
    }

    @Test
    @DisplayName("上下架成功：上架(1) 切换到下架(0)")
    void togglePublish_onToOff() {
        when(blogInfoMapper.selectById(1)).thenReturn(buildBlog(1, "1"));

        blogService.togglePublish(1, 1);

        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getPublishedStatus());
    }
}
