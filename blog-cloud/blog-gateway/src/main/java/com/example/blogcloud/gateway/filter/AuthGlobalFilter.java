package com.example.blogcloud.gateway.filter;

import com.example.blogcloud.common.constant.Constants;
import com.example.blogcloud.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局鉴权过滤器
 * <p>
 * 组件三(Gateway)演示:
 * 1. 白名单(登录/注册/静态资源)直接放行;
 * 2. 其余 /user/** 与 /blog/** 请求校验 JWT, 无效返回 401;
 * 3. 校验通过后把 userId / userName 放进请求头转发给下游服务(下游不再解析 token)。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 无需登录即可访问的路径(前缀匹配) */
    private static final List<String> WHITELIST = List.of(
            // 用户接口
            "/user/login", "/user/register", "/user/configInfo",
            // 静态页面
            "/blog_list.html", "/blog_detail.html", "/blog_edit.html", "/blog_update.html",
            "/blog_login.html", "/blog_register.html", "/blog_admin.html",
            // 静态资源
            "/css/", "/js/", "/pic/", "/blog-editormd/", "/favicon.ico", "/tuiao.png"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 只对业务接口做鉴权, 其他路径(含静态资源)直接放行
        boolean isBusiness = path.startsWith("/user/") || path.startsWith("/blog/");
        if (!isBusiness || isWhitelist(path)) {
            return chain.filter(exchange);
        }

        // 从请求头取 token(兼容新旧字段名)
        String token = request.getHeaders().getFirst(Constants.TOKEN);
        if (token == null || token.isEmpty()) {
            token = request.getHeaders().getFirst(Constants.TOKEN_OLD);
        }

        Claims claims = JwtUtils.parseJwt(token);
        if (claims == null) {
            return unauthorized(exchange);
        }

        // 校验通过: 把用户信息注入请求头, 转发给下游服务
        ServerHttpRequest mutated = request.mutate()
                .header(Constants.HEADER_USER_ID, String.valueOf(JwtUtils.getUserIdFromToken(token)))
                .header(Constants.HEADER_USER_NAME, JwtUtils.getUserNameFromToken(token))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isWhitelist(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    /** 返回 401 JSON */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = "{\"code\":-1,\"errMsg\":\"未登录或登录已失效\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
