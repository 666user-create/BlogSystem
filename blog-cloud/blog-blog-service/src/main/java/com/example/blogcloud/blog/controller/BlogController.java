package com.example.blogcloud.blog.controller;

import com.example.blogcloud.blog.service.BlogService;
import com.example.blogcloud.common.constant.Constants;
import com.example.blogcloud.common.exception.BlogException;
import com.example.blogcloud.common.pojo.dataObject.BlogInfo;
import com.example.blogcloud.common.pojo.response.BlogInfoResponse;
import com.example.blogcloud.common.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 博客相关接口
 * <p>
 * 微服务版鉴权说明: 正常入口是网关, 网关校验 JWT 后把用户信息放入
 * X-User-Id / X-User-Name 请求头转发下来, 本服务直接信任该请求头;
 * 为方便绕过网关直接调试, 请求头缺失时回退到解析 token。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource
    private BlogService blogService;

    @Resource
    private HttpServletRequest request;

    /** 获取全部已上架博客列表 */
    @GetMapping("/getList")
    public List<BlogInfoResponse> getList() {
        log.info("获取全部博客列表");
        return blogService.getList();
    }

    /** 获取博客详情(含作者名, 作者名跨服务获取) */
    @GetMapping("/getBlogDetail")
    public BlogInfoResponse getBlogDetail(@RequestParam @NotNull(message = "博客id不能为空")
                                          @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        log.info("获取博客详情, blogId: {}", blogId);
        return blogService.getBlogDetail(blogId);
    }

    /** 新增博客 */
    @PostMapping("/add")
    public Boolean addBlog(@RequestBody @Validated BlogInfoResponse blogRequest) {
        Integer userId = getUserId();
        log.info("用户 {} 发表博客, 标题: {}", userId, blogRequest.getTitle());
        blogService.addBlog(blogRequest.getTitle(), blogRequest.getContent(), userId);
        return true;
    }

    /** 更新博客 */
    @PostMapping("/update")
    public Boolean updateBlog(@RequestBody @Validated BlogInfoResponse blogRequest) {
        Integer userId = getUserId();
        log.info("用户 {} 更新博客, id: {}", userId, blogRequest.getId());
        blogService.updateBlog(blogRequest.getId(), blogRequest.getTitle(), blogRequest.getContent(), userId);
        return true;
    }

    /** 删除博客(逻辑删除) */
    @PostMapping("/delete")
    public Boolean deleteBlog(@RequestParam @NotNull(message = "博客id不能为空")
                              @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        Integer userId = getUserId();
        log.info("用户 {} 删除博客, id: {}", userId, blogId);
        blogService.deleteBlog(blogId, userId);
        return true;
    }

    /** 管理员获取全部博客列表(含下架) */
    @GetMapping("/adminList")
    public List<BlogInfoResponse> adminList() {
        checkAdmin();
        log.info("管理员获取全部博客列表");
        return blogService.adminList();
    }

    /** 管理员切换博客上下架 */
    @PostMapping("/togglePublish")
    public Boolean togglePublish(@RequestParam @NotNull(message = "博客id不能为空")
                                 @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        checkAdmin();
        log.info("管理员切换博客 {} 上下架", blogId);
        blogService.togglePublish(blogId, getUserId());
        return true;
    }

    /**
     * 内部接口: 返回博客数据对象(供 user-service 的 Feign 调用)。
     * 路径含 /internal/ 时不套 Result 壳(见 ResponseAdvice)。
     */
    @GetMapping("/internal/getBlogInfo")
    public BlogInfo getBlogInfo(@RequestParam @NotNull(message = "博客id不能为空") Integer blogId) {
        log.info("内部接口: 获取博客数据 blogId={}", blogId);
        return blogService.getBlogInfo(blogId);
    }

    // ===== 用户身份获取: 优先网关注入的请求头, 缺失时回退解析 token =====

    private Integer getUserId() {
        String headerUserId = request.getHeader(Constants.HEADER_USER_ID);
        if (headerUserId != null && !headerUserId.isEmpty()) {
            return Integer.valueOf(headerUserId);
        }
        String token = getToken();
        Integer userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BlogException("登录已失效，请重新登录");
        }
        return userId;
    }

    private void checkAdmin() {
        String headerUserName = request.getHeader(Constants.HEADER_USER_NAME);
        if (headerUserName != null && !headerUserName.isEmpty()) {
            if (!"admin".equals(headerUserName)) {
                throw new BlogException("无管理员权限");
            }
            return;
        }
        String userName = JwtUtils.getUserNameFromToken(getToken());
        if (!"admin".equals(userName)) {
            throw new BlogException("无管理员权限");
        }
    }

    private String getToken() {
        String token = request.getHeader(Constants.TOKEN);
        if (token == null || token.isEmpty()) {
            token = request.getHeader(Constants.TOKEN_OLD);
        }
        return token;
    }
}
