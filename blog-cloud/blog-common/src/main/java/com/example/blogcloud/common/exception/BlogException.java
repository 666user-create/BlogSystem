package com.example.blogcloud.common.exception;

/**
 * 业务异常: 由全局异常处理器统一转换为 Result.fail 返回
 */
public class BlogException extends RuntimeException {
    public BlogException(String message) {
        super(message);
    }
}
