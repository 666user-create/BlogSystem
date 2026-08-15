package com.example.blogcloud.common.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.blogcloud.common.pojo.response.Result;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应包装切面: 把 Controller 返回值统一包装成 Result
 * <p>
 * 微服务版注意点: 路径包含 "/internal/" 的内部接口(服务间 Feign 调用)不包装,
 * 直接返回原始对象 —— 否则 Feign 收到的是 {"code":200,"data":{...}} 而不是业务对象本身。
 */
@ControllerAdvice
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @SneakyThrows
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 内部接口(服务间调用)直接返回原始对象, 不套 Result 壳
        if (request.getURI().getPath().contains("/internal/")) {
            return body;
        }

        if (body instanceof Result) {
            return body;
        }
        // String 需要特殊处理为 JSON 字符串, 否则 StringHttpMessageConverter 会报错
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(Result.success(body));
        }
        return Result.success(body);
    }
}
