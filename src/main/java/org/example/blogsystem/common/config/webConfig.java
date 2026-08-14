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
     * 拦截 /blog/** 和 /user/** 接口，放行 /user/login 和 /user/register。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/blog/**","/user/**")
                .excludePathPatterns("/user/login","/user/register");
    }
}
