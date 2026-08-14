package org.example.blogsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.pojo.dataObject.BlogInfo;
import org.example.blogsystem.common.pojo.response.BlogInfoResponse;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.utils.MyBeanUtils;
import org.example.blogsystem.mapper.BlogInfoMapper;
import org.example.blogsystem.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 博客业务实现类
 * <p>
 * 负责博客的查询、新增、更新、删除（逻辑删除）等具体操作。
 */
@Slf4j
@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogInfoMapper blogInfoMapper;
    /**
     * 获取未删除的博客列表
     */
    @Override
    public List<BlogInfoResponse> getList() {
        QueryWrapper<BlogInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getDeleteFlag,0)
                .eq(BlogInfo::getPublishedStatus,1)
                .orderByDesc(BlogInfo::getId);
        List<BlogInfo> blogInfos=blogInfoMapper.selectList(queryWrapper);
        return blogInfos.stream()
                .map(MyBeanUtils::transBlogInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定用户未删除的博客列表
     */
    @Override
    public List<BlogInfoResponse> getListByUserId(Integer userId) {
        QueryWrapper<BlogInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getDeleteFlag,0)
                .eq(BlogInfo::getUserId, String.valueOf(userId))
                .orderByDesc(BlogInfo::getId);
        List<BlogInfo> blogInfos=blogInfoMapper.selectList(queryWrapper);
        return blogInfos.stream()
                .map(MyBeanUtils::transBlogInfo)
                .collect(Collectors.toList());
    }

    /**
     * 根据 id 获取博客详情
     */
    @Override
    public BlogInfoResponse getBlogDetail(@NotNull Integer blogId) {
        BlogInfo blogInfo = getBlogInfo(blogId);
        if (blogInfo == null) {
            throw new BlogException("博客不存在");
        }
        return MyBeanUtils.transBlogInfo(blogInfo);
    }

    /**
     * 根据 id 获取博客数据对象（仅查询未删除）
     */
    @Override
    public BlogInfo getBlogInfo(@NotNull Integer blogId){
        QueryWrapper<BlogInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.lambda().eq(BlogInfo::getDeleteFlag,0)
                .eq(BlogInfo::getId,blogId);
        return blogInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 新增博客
     */
    @Override
    public void addBlog(String title, String content, Integer userId) {
        if (title == null || title.trim().isEmpty()) {
            throw new BlogException("标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BlogException("内容不能为空");
        }
        if (userId == null) {
            throw new BlogException("用户未登录");
        }
        BlogInfo blogInfo = new BlogInfo();
        blogInfo.setTitle(title.trim());
        blogInfo.setContent(content);
        blogInfo.setUserId(String.valueOf(userId));
        blogInfo.setDeleteFlag(0);
        blogInfo.setPublishedStatus(1);
        LocalDateTime nowTime = LocalDateTime.now();
        blogInfo.setCreateTime(nowTime);
        blogInfo.setUpdateTime(nowTime);
        blogInfoMapper.insert(blogInfo);
    }

    /**
     * 更新博客内容
     */
    @Override
    public void updateBlog(Integer blogId, String title, String content, Integer userId) {
        if (blogId == null) {
            throw new BlogException("博客id不能为空");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new BlogException("标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BlogException("内容不能为空");
        }
        if (userId == null) {
            throw new BlogException("用户未登录");
        }

        BlogInfo blogInfo = getBlogInfo(blogId);
        if (blogInfo == null) {
            throw new BlogException("博客不存在");
        }
        if (!String.valueOf(userId).equals(blogInfo.getUserId())) {
            throw new BlogException("无权编辑该博客");
        }

        blogInfo.setTitle(title.trim());
        blogInfo.setContent(content);
        blogInfo.setUpdateTime(LocalDateTime.now());
        blogInfoMapper.updateById(blogInfo);
    }

    /**
     * 删除博客（逻辑删除，将 deleteFlag 置为 1）
     */
    @Override
    public void deleteBlog(Integer blogId, Integer userId) {
        if (blogId == null) {
            throw new BlogException("博客id不能为空");
        }
        if (userId == null) {
            throw new BlogException("用户未登录");
        }

        BlogInfo blogInfo = getBlogInfo(blogId);
        if (blogInfo == null) {
            throw new BlogException("博客不存在");
        }
        if (!String.valueOf(userId).equals(blogInfo.getUserId())) {
            throw new BlogException("无权删除该博客");
        }

        blogInfo.setDeleteFlag(1);
        blogInfo.setUpdateTime(LocalDateTime.now());
        blogInfoMapper.updateById(blogInfo);
    }

    /**
     * 管理员获取全部博客列表（未删除，含上架、下架）
     */
    @Override
    public List<BlogInfoResponse> adminList() {
        QueryWrapper<BlogInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getDeleteFlag, 0)
                .orderByDesc(BlogInfo::getId);
        List<BlogInfo> blogInfos = blogInfoMapper.selectList(queryWrapper);
        return blogInfos.stream()
                .map(MyBeanUtils::transBlogInfo)
                .collect(Collectors.toList());
    }

    /**
     * 管理员切换博客上下架状态
     */
    @Override
    public void togglePublish(Integer blogId, Integer adminUserId) {
        if (blogId == null) {
            throw new BlogException("博客id不能为空");
        }
        BlogInfo blogInfo = blogInfoMapper.selectById(blogId);
        if (blogInfo == null || blogInfo.getDeleteFlag() == 1) {
            throw new BlogException("博客不存在");
        }
        int newStatus = (blogInfo.getPublishedStatus() != null && blogInfo.getPublishedStatus() == 1) ? 0 : 1;
        blogInfo.setPublishedStatus(newStatus);
        blogInfo.setUpdateTime(LocalDateTime.now());
        blogInfoMapper.updateById(blogInfo);
        log.info("管理员 {} 切换博客 {} 状态为: {}", adminUserId, blogId, newStatus == 1 ? "上架" : "下架");
    }

    /**
     * 根据用户 id 统计其未删除的博客数量
     */
    @Override
    public Integer countByUserId(Integer userId) {
        QueryWrapper<BlogInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getUserId, String.valueOf(userId))
                .eq(BlogInfo::getDeleteFlag, 0);
        return blogInfoMapper.selectCount(queryWrapper).intValue();
    }
}
