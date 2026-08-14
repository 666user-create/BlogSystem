package org.example.blogsystem.common.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.constant.Constants;
import org.example.blogsystem.common.utils.JwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器
 * <p>
 * 拦截受保护的接口，从请求头中读取 JWT（userToken），
 * 如果 token 为空或解析失败，则返回 401，阻止后续 Controller 执行。
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(Constants.TOKEN);
        if (token == null || token.isEmpty()) {
            token = request.getHeader(Constants.TOKEN_OLD);
        }
        log.info("获取到的token:{}",token);
        if (token == null || token.isEmpty()) {
            // token 为空，认为未登录，直接返回 401
            response.setStatus(401);
            return false;
        }
        // 校验 token 是否有效（是否被篡改、是否过期等）
        Claims claims= JwtUtils.parseJwt(token);
        if(claims==null){
            // 解析失败，同样返回 401
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
