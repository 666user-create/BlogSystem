package org.example.blogsystem.common.advice;

import com.fasterxml.jackson.databind.ObjectMapper; // 修正点1：必须是 com.fasterxml...
import lombok.SneakyThrows;
import org.example.blogsystem.common.pojo.response.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice; // 或 @RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应包装切面
 * <p>
 * 将 Controller 的返回统一包装成 Result：
 * - 已经是 Result 类型的，直接返回
 * - String 需要特殊处理为 JSON 字符串
 * - 其他对象统一包装为 Result.success(body)
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
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        // 1. 如果返回已经是 Result 类型，直接返回，避免重复包装
        if (body instanceof Result) {
            return body;
        }

        // 2. 特殊处理 String 类型 (修正点3)
        // SpringMVC 对于 String 返回值默认使用 StringHttpMessageConverter，它只接受 String
        // 如果直接返回 Result 对象会报 ClassCastException
        // 且必须手动设置 Content-Type 为 JSON，否则前端收到的是 text/plain
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return objectMapper.writeValueAsString(Result.success(body));
        }

        // 3. 正常包装其他对象
        return Result.success(body);
    }
}