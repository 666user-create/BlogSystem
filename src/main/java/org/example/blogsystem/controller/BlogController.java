package org.example.blogsystem.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.constant.Constants;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.pojo.response.BlogInfoResponse;
import org.example.blogsystem.common.pojo.response.PageResult;
import org.example.blogsystem.common.utils.JwtUtils;
import org.example.blogsystem.service.BlogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 博客相关接口
 * <p>
 * 提供：获取博客列表、获取详情、新增、更新、删除博客等接口。
 */
@CrossOrigin(origins = "*")
@Slf4j
@Validated
@RestController
@RequestMapping("/blog")
public class BlogController {
    @Resource(name = "blogServiceImpl")
    private BlogService blogService;

    @Resource
    private HttpServletRequest request;

    /**
     * 分页获取博客列表（仅已上架、未删除，按 id 倒序）
     *
     * @param pageNum  页码，从 1 开始，默认 1
     * @param pageSize 每页条数，默认 10，上限 50（防止一次拉取过多数据）
     */
    @GetMapping("/getList")
    public PageResult<BlogInfoResponse> getList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于0")
            @Max(value = 50, message = "每页条数不能超过50") Integer pageSize) {
        log.info("分页获取博客列表, pageNum={}, pageSize={}", pageNum, pageSize);
        return blogService.getList(pageNum, pageSize);
    }

    /**
     * 获取博客详情
     */
    @GetMapping("/getBlogDetail")
    public BlogInfoResponse getBlogDetail(@RequestParam @NotNull(message = "博客id不能为空") @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        log.info("获取博客详情, blogId: {}", blogId);
        return blogService.getBlogDetail(blogId);
    }

    /**
     * 新增博客
     */
    @PostMapping("/add")
    public Boolean addBlog(@RequestBody @Validated BlogInfoResponse blogRequest) {
        Integer userId = getUserIdFromToken();
        log.info("用户 {} 发表博客, 标题: {}", userId, blogRequest.getTitle());
        blogService.addBlog(blogRequest.getTitle(), blogRequest.getContent(), userId);
        return true;
    }

    /**
     * 更新博客
     */
    @PostMapping("/update")
    public Boolean updateBlog(@RequestBody @Validated BlogInfoResponse blogRequest) {
        Integer userId = getUserIdFromToken();
        log.info("用户 {} 更新博客, id: {}", userId, blogRequest.getId());
        blogService.updateBlog(blogRequest.getId(), blogRequest.getTitle(), blogRequest.getContent(), userId);
        return true;
    }

    /**
     * 删除博客
     */
    @PostMapping("/delete")
    public Boolean deleteBlog(@RequestParam @NotNull(message = "博客id不能为空") @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        Integer userId = getUserIdFromToken();
        log.info("用户 {} 删除博客, id: {}", userId, blogId);
        blogService.deleteBlog(blogId, userId);
        return true;
    }

    /**
     * 管理员分页获取全部博客列表（含下架）
     */
    @GetMapping("/adminList")
    public PageResult<BlogInfoResponse> adminList(
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") Integer pageNum,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数必须大于0")
            @Max(value = 50, message = "每页条数不能超过50") Integer pageSize) {
        checkAdmin();
        log.info("管理员分页获取博客列表, pageNum={}, pageSize={}", pageNum, pageSize);
        return blogService.adminList(pageNum, pageSize);
    }

    /**
     * 管理员切换博客上下架
     */
    @PostMapping("/togglePublish")
    public Boolean togglePublish(@RequestParam @NotNull(message = "博客id不能为空") @Min(value = 1, message = "博客id必须大于0") Integer blogId) {
        checkAdmin();
        log.info("管理员切换博客 {} 上下架", blogId);
        blogService.togglePublish(blogId, getUserIdFromToken());
        return true;
    }

    /**
     * 校验当前用户是否为管理员（admin）
     */
    private void checkAdmin() {
        String token = request.getHeader(Constants.TOKEN);
        String userName = JwtUtils.getUserNameFromToken(token);
        if (!"admin".equals(userName)) {
            throw new BlogException("无管理员权限");
        }
    }

    private Integer getUserIdFromToken() {
        String token = request.getHeader(Constants.TOKEN);
        Integer userId = JwtUtils.getUserIdFromToken(token);
        if (userId == null) {
            throw new BlogException("登录已失效，请重新登录");
        }
        return userId;
    }
}
