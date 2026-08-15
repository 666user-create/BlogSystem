package com.example.blogcloud.gateway.filter;

import com.example.blogcloud.common.constant.Constants;
import com.example.blogcloud.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthGlobalFilter 单元测试（网关全局鉴权过滤器）
 * ============================================================
 * 覆盖（对应实现文档的网关鉴权规则）：
 *   1. 白名单路径（登录/注册/静态资源）直接放行，不校验 token；
 *   2. 非业务路径直接放行；
 *   3. 业务接口无 token / 篡改 token → 返回 401；
 *   4. 有效 token → 放行，并把 userId / userName 注入请求头转发给下游。
 *
 * 用 Mockito mock WebFlux 的接口（ServerWebExchange / GatewayFilterChain），
 * 不启动网关、不依赖 Nacos，纯逻辑验证。
 * ============================================================
 */
class AuthGlobalFilterTest {

    private static final String SECRET = "dVnsmy+SIX6pNptQdeclDSJ26EMSPEIhvZYKBTTug4k=";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L;

    private final AuthGlobalFilter filter = new AuthGlobalFilter();

    /** mock 的过滤器链（表示"放行，继续往下游执行"） */
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        JwtUtils.init(SECRET, EXPIRATION);
        chain = mock(GatewayFilterChain.class);
        // 过滤器链放行时返回 Mono.empty()（WebFlux 异步完成信号）
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    /**
     * 构造一个访问指定路径的 mock 请求交换对象
     *
     * @param path  请求路径，如 "/user/getUserInfo"
     * @param token 请求头里的 token（可传 null 表示不带）
     */
    private ServerWebExchange buildExchange(String path, String token) {
        // request 用 mock（保证后续可 stub mutate() 等行为），token 放在真实的 HttpHeaders 里
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set(Constants.TOKEN, token);
        }
        when(request.getURI()).thenReturn(URI.create("http://localhost" + path));
        when(request.getHeaders()).thenReturn(headers);

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        when(exchange.getRequest()).thenReturn(request);

        // response 用 mock：记录 setStatusCode 调用；401 时会写 Content-Type 和 body
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(response.getHeaders()).thenReturn(new HttpHeaders());
        // 用真实的 DefaultDataBufferFactory，让 401 的 writeWith 能拿到真实 DataBuffer
        when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());
        when(response.writeWith(any())).thenReturn(Mono.empty());
        when(exchange.getResponse()).thenReturn(response);
        return exchange;
    }

    // ==================== 放行场景 ====================

    @Test
    @DisplayName("白名单：登录接口不校验 token 直接放行")
    void whitelist_login_pass() {
        ServerWebExchange exchange = buildExchange("/user/login", null);

        filter.filter(exchange, chain).block();   // block() 触发过滤器执行

        verify(chain).filter(exchange);           // 放行到下游
    }

    @Test
    @DisplayName("白名单：静态页面直接放行")
    void whitelist_staticPage_pass() {
        ServerWebExchange exchange = buildExchange("/blog_list.html", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("非业务路径（如静态资源）直接放行")
    void nonBusinessPath_pass() {
        ServerWebExchange exchange = buildExchange("/css/common.css", null);

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }

    // ==================== 401 场景 ====================

    @Test
    @DisplayName("业务接口无 token 返回 401，不转发下游")
    void businessPath_noToken_unauthorized() {
        ServerWebExchange exchange = buildExchange("/user/getUserInfo", null);

        filter.filter(exchange, chain).block();

        // 响应被置为 401，且没有继续执行过滤器链
        verify(exchange.getResponse()).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    @DisplayName("业务接口篡改 token 返回 401")
    void businessPath_tamperedToken_unauthorized() {
        String valid = JwtUtils.genJwt(buildClaims());
        String tampered = valid.substring(0, valid.length() - 4) + "xxxx";

        ServerWebExchange exchange = buildExchange("/blog/getList", tampered);

        filter.filter(exchange, chain).block();

        verify(exchange.getResponse()).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    // ==================== 有效 token 场景 ====================

    @Test
    @DisplayName("有效 token：放行并把 userId/userName 注入请求头")
    void businessPath_validToken_passAndInjectHeaders() {
        String token = JwtUtils.genJwt(buildClaims());
        ServerWebExchange exchange = buildExchange("/blog/add", token);

        // 模拟 request.mutate()：网关会把新请求头拼进请求再转发
        ServerHttpRequest.Builder builder = mock(ServerHttpRequest.Builder.class);
        when(builder.header(anyString(), any(String[].class))).thenReturn(builder);
        ServerHttpRequest mutated = mock(ServerHttpRequest.class);
        when(builder.build()).thenReturn(mutated);

        ServerHttpRequest request = exchange.getRequest();
        when(request.mutate()).thenReturn(builder);

        // 模拟 exchange.mutate()：过滤器会把"带新请求头的请求"包回 exchange 再放行
        ServerWebExchange.Builder exBuilder = mock(ServerWebExchange.Builder.class);
        when(exBuilder.request(mutated)).thenReturn(exBuilder);
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exBuilder.build()).thenReturn(mutatedExchange);
        when(exchange.mutate()).thenReturn(exBuilder);

        filter.filter(exchange, chain).block();

        // 校验通过：注入了用户信息请求头，并放行
        verify(builder).header(Constants.HEADER_USER_ID, "1");
        verify(builder).header(Constants.HEADER_USER_NAME, "zhangsan");
        verify(chain).filter(any(ServerWebExchange.class));
        // 不应返回 401
        verify(exchange.getResponse(), never()).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    private Map<String, Object> buildClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", 1);
        claims.put("name", "zhangsan");
        return claims;
    }
}
