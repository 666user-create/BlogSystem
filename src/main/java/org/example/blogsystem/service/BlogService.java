package org.example.blogsystem.service;

import jakarta.validation.constraints.NotNull;
import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.example.blogsystem.common.pojo.response.BlogInfoResponse;
import org.example.blogsystem.common.pojo.response.PageResult;

import java.util.List;

public interface BlogService {
    /**
     * 分页获取已上架、未删除的博客列表（按 id 倒序）
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     */
    PageResult<BlogInfoResponse> getList(int pageNum, int pageSize);

    /**
     * 根据用户 id 获取该用户未删除的博客列表
     */
    List<BlogInfoResponse> getListByUserId(Integer userId);

    BlogInfoResponse getBlogDetail(Integer blogId);

    BlogInfo getBlogInfo(Integer blogId);

    /**
     * 新增博客
     */
    void addBlog(String title, String content, Integer userId);

    /**
     * 更新博客
     */
    void updateBlog(Integer blogId, String title, String content, Integer userId);

    /**
     * 删除博客（逻辑删除）
     */
    void deleteBlog(Integer blogId, Integer userId);

    /**
     * 根据用户 id 统计该用户未删除的博客数量
     */
    Integer countByUserId(Integer userId);

    /**
     * 管理员分页获取全部未删除的博客列表（含下架）
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     */
    PageResult<BlogInfoResponse> adminList(int pageNum, int pageSize);

    /**
     * 管理员切换博客上下架状态
     */
    void togglePublish(Integer blogId, Integer adminUserId);
}
