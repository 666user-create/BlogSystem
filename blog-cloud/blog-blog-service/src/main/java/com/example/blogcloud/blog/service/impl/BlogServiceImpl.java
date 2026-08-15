package com.example.blogcloud.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.blogcloud.blog.client.UserClient;
import com.example.blogcloud.blog.mapper.BlogInfoMapper;
import com.example.blogcloud.blog.service.AuthorInfoService;
import com.example.blogcloud.blog.service.BlogService;
import com.example.blogcloud.common.exception.BlogException;
import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.message.BlogPublishMessage;
import com.example.blogcloud.common.pojo.response.BlogInfoResponse;
import com.example.blogcloud.common.utils.MyBeanUtils;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 博客业务实现
 */
@Slf4j
@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogInfoMapper blogInfoMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private AuthorInfoService authorInfoService;

    @Autowired
    private StreamBridge streamBridge;

    /** RocketMQ 生产开关: 默认 false, 激活 mq profile 后为 true(见 application-mq.yml) */
    @Value("${app.mq.enabled:false}")
    private boolean mqEnabled;

    @Override
    public List<BlogInfoResponse> getList() {
        QueryWrapper<BlogInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getDeleteFlag, 0)
                .eq(BlogInfo::getPublishedStatus, 1)
                .orderByDesc(BlogInfo::getId);
        return blogInfoMapper.selectList(queryWrapper).stream()
                .map(MyBeanUtils::transBlogInfo)
                .collect(Collectors.toList());
    }

    @Override
    public BlogInfoResponse getBlogDetail(Integer blogId) {
        BlogInfo blogInfo = getBlogInfo(blogId);
        if (blogInfo == null) {
            throw new BlogException("博客不存在");
        }
        BlogInfoResponse response = MyBeanUtils.transBlogInfo(blogInfo);
        // 作者名跨服务获取(Feign + Sentinel 兜底)
        response.setAuthorName(authorInfoService.getAuthorName(blogInfo.getUserId()));
        return response;
    }

    @Override
    public BlogInfo getBlogInfo(Integer blogId) {
        QueryWrapper<BlogInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(BlogInfo::getDeleteFlag, 0)
                .eq(BlogInfo::getId, blogId);
        return blogInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 新增博客 —— 组件五(Seata)全局事务入口
     * <p>
     * 开启 Seata 时: 写 blog_info + Feign 调 user-service 增加 blog_count 处于同一全局事务,
     * 任一分支失败整体回滚。Seata 未开启时注解不生效, 退化为普通调用。
     */
    @Override
    @GlobalTransactional(name = "blog-add-transaction", rollbackFor = Exception.class)
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
        log.info("博客已写入: id={}, userId={}", blogInfo.getId(), userId);

        // Seata 全局事务分支: 远程调用用户服务把作者的博客数 +1
        Boolean ok = userClient.increaseBlogCount(userId);
        log.info("远程增加用户博客数结果: {}", ok);
    }

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
        log.info("博客已删除(逻辑删除): id={}", blogId);

        // 组件四(RocketMQ)演示: 删除成功后向 blog-topic 发送事件, 由 user-service 异步把作者博客数 -1
        if (mqEnabled) {
            BlogPublishMessage message = new BlogPublishMessage(
                    blogId, blogInfo.getTitle(), Integer.valueOf(blogInfo.getUserId()));
            boolean sent = streamBridge.send("blog-out-0", message);
            log.info("【RocketMQ】发送博客删除事件: blogId={}, 发送结果={}", blogId, sent);
        }
    }

    @Override
    public List<BlogInfoResponse> adminList() {
        QueryWrapper<BlogInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(BlogInfo::getDeleteFlag, 0)
                .orderByDesc(BlogInfo::getId);
        return blogInfoMapper.selectList(queryWrapper).stream()
                .map(MyBeanUtils::transBlogInfo)
                .collect(Collectors.toList());
    }

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
}
