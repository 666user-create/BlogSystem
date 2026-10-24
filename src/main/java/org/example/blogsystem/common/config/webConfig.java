package org.example.blogsystem.common.config;

import org.example.blogsystem.common.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * <p>
 * 用于注册 SpringMVC 的拦截器等组件。
 */
@Configuration
public class webConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    /**
     * 注册登录拦截器
     * <p>
     * 拦截 /blog/** 和 /user/** 接口；放行：
     * <ul>
     *   <li>/user/login、/user/register：登录注册本身</li>
     *   <li>/blog/getList、/blog/getBlogDetail：游客可浏览（BUG-07 修复）——
     *       列表与详情是只读内容，允许未登录用户查看；写操作、管理端、用户接口仍需登录</li>
     * </ul>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/blog/**","/user/**")
                .excludePathPatterns("/user/login","/user/register",
                        "/blog/getList","/blog/getBlogDetail");
    }
}
