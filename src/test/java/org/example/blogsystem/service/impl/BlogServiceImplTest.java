package org.example.blogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.example.blogsystem.common.pojo.response.BlogInfoResponse;
import org.example.blogsystem.common.pojo.response.PageResult;
import org.example.blogsystem.mapper.BlogInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BlogServiceImpl 单元测试
 * ============================================================
 * 和 UserServiceImplTest 一样的思路：mock 掉数据库 Mapper，
 * 专注验证 BlogServiceImpl 的业务逻辑：
 *   - 参数校验（标题/内容为空、id 为空）
 *   - 权限校验（只有作者能改/删自己的博客）
 *   - 业务规则（新增默认上架、删除是逻辑删除 delete_flag=1、上下架切换）
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class BlogServiceImplTest {

    /** 假的博客表 Mapper */
    @Mock
    private BlogInfoMapper blogInfoMapper;

    /** 被测对象 */
    @InjectMocks
    private BlogServiceImpl blogService;

    /** 构造一篇 userA(用户id=1) 写的博客 */
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

    // ==================== 新增博客（addBlog） ====================

    @Test
    @DisplayName("新增失败：标题为空")
    void addBlog_emptyTitle() {
        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.addBlog("   ", "内容", 1));
        assertEquals("标题不能为空", ex.getMessage());
        verify(blogInfoMapper, never()).insert(any(BlogInfo.class));   // 没执行插入
    }

    @Test
    @DisplayName("新增失败：内容为空")
    void addBlog_emptyContent() {
        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.addBlog("标题", null, 1));
        assertEquals("内容不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("新增成功：默认上架、逻辑删除标记为0、标题去空格")
    void addBlog_success() {
        blogService.addBlog("  新博客  ", "第一篇内容", 1);

        // 捕获 insert 的参数，验证字段设置是否符合业务规则
        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).insert(captor.capture());

        BlogInfo saved = captor.getValue();
        assertEquals("新博客", saved.getTitle());          // 标题已 trim 去空格
        assertEquals("1", saved.getUserId());              // userId 存为字符串
        assertEquals(1, saved.getPublishedStatus());       // 新博客默认上架
        assertEquals(0, saved.getDeleteFlag());            // 未删除
    }

    // ==================== 更新博客（updateBlog） ====================

    @Test
    @DisplayName("更新失败：非作者更新他人博客")
    void updateBlog_notOwner() {
        // 数据库里博客 id=1 是 user 1 写的
        when(blogInfoMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(buildBlog(1, "1"));

        // 用户 2 试图更新 → 无权
        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.updateBlog(1, "新标题", "新内容", 2));
        assertEquals("无权编辑该博客", ex.getMessage());
        verify(blogInfoMapper, never()).updateById(any(BlogInfo.class));
    }

    @Test
    @DisplayName("更新失败：博客不存在")
    void updateBlog_notExist() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.updateBlog(999, "标题", "内容", 1));
        assertEquals("博客不存在", ex.getMessage());
    }

    @Test
    @DisplayName("更新成功：作者更新自己的博客")
    void updateBlog_owner_success() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(buildBlog(1, "1"));

        blogService.updateBlog(1, "新标题", "新内容", 1);

        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals("新标题", captor.getValue().getTitle());
    }

    // ==================== 删除博客（deleteBlog） ====================

    @Test
    @DisplayName("删除失败：非作者删除他人博客")
    void deleteBlog_notOwner() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(buildBlog(1, "1"));

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.deleteBlog(1, 2));
        assertEquals("无权删除该博客", ex.getMessage());
    }

    @Test
    @DisplayName("删除成功：逻辑删除（delete_flag 置 1，记录保留）")
    void deleteBlog_success() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(buildBlog(1, "1"));

        blogService.deleteBlog(1, 1);

        // 关键断言：是 updateById（改标记），而不是 deleteById（物理删除）
        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getDeleteFlag(), "逻辑删除标记应为 1");
    }

    // ==================== 上下架（togglePublish） ====================

    @Test
    @DisplayName("上下架失败：已删除的博客不能操作")
    void togglePublish_deletedBlog() {
        BlogInfo deleted = buildBlog(1, "1");
        deleted.setDeleteFlag(1);   // 已删除
        when(blogInfoMapper.selectById(1)).thenReturn(deleted);

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.togglePublish(1, 1));
        assertEquals("博客不存在", ex.getMessage());
    }

    @Test
    @DisplayName("上下架成功：上架(1) 切换到下架(0)")
    void togglePublish_onToOff() {
        when(blogInfoMapper.selectById(1)).thenReturn(buildBlog(1, "1"));  // publishedStatus=1

        blogService.togglePublish(1, 1);

        ArgumentCaptor<BlogInfo> captor = ArgumentCaptor.forClass(BlogInfo.class);
        verify(blogInfoMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getPublishedStatus(), "上架应切换为下架");
    }

    // ==================== 列表 / 详情 / 统计 ====================

    @Test
    @DisplayName("列表查询：返回当前页数据，并透传分页信息")
    void getList_success() {
        BlogInfo b1 = buildBlog(1, "1");
        BlogInfo b2 = buildBlog(2, "1");
        Page<BlogInfo> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(b1, b2));
        mockPage.setTotal(2);

        when(blogInfoMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        PageResult<BlogInfoResponse> result = blogService.getList(1, 10);

        assertEquals(2, result.getList().size());
        assertEquals(1, result.getList().get(0).getId());
        assertEquals("标题", result.getList().get(0).getTitle());
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getPages());
    }

    @Test
    @DisplayName("列表查询：无数据时返回空列表、total=0、pages=0（不抛异常）")
    void getList_empty() {
        Page<BlogInfo> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(0);

        when(blogInfoMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(emptyPage);

        PageResult<BlogInfoResponse> result = blogService.getList(1, 10);

        assertEquals(0, result.getList().size());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getPages());
    }

    @Test
    @DisplayName("列表查询：总页数按向上取整计算（25 条 / 每页 10 条 = 3 页）")
    void getList_pagesRoundedUp() {
        Page<BlogInfo> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(buildBlog(1, "1")));
        mockPage.setTotal(25);

        when(blogInfoMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockPage);

        PageResult<BlogInfoResponse> result = blogService.getList(1, 10);

        assertEquals(25, result.getTotal());
        assertEquals(3, result.getPages(), "25 条数据每页 10 条应算出 3 页");
    }

    @Test
    @DisplayName("列表查询：页码超出范围时返回空列表，但 total 仍准确（末页之后的越界访问）")
    void getList_pageOutOfRange() {
        Page<BlogInfo> emptyPage = new Page<>(99, 10);   // 第 99 页，实际只有 2 页
        emptyPage.setRecords(Collections.emptyList());
        emptyPage.setTotal(15);

        when(blogInfoMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(emptyPage);

        PageResult<BlogInfoResponse> result = blogService.getList(99, 10);

        assertEquals(0, result.getList().size(), "越界页码应返回空列表");
        assertEquals(15, result.getTotal(), "total 必须仍然准确");
        assertEquals(2, result.getPages());
    }

    @Test
    @DisplayName("详情查询：博客不存在抛异常")
    void getBlogDetail_notExist() {
        when(blogInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        BlogException ex = assertThrows(BlogException.class,
                () -> blogService.getBlogDetail(999));
        assertEquals("博客不存在", ex.getMessage());
    }

    @Test
    @DisplayName("统计用户博客数：透传 mapper 的 count 结果")
    void countByUserId_success() {
        when(blogInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(5L);
        assertEquals(5, blogService.countByUserId(1));
    }
}
