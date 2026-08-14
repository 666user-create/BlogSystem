package org.example.blogsystem.common.advice;

import lombok.extern.slf4j.Slf4j;
import org.example.blogsystem.common.exception.BlogException;
import org.example.blogsystem.common.pojo.response.Result;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 * <p>
 * 捕获项目中抛出的异常，统一转换为 Result.fail 返回给前端，
 * 避免异常栈直接暴露给用户。
 */
@ResponseBody
@Slf4j
@ControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(value = Exception.class)
    public Result exceptionHandler(Exception exception) {
        log.error(exception.getMessage(), exception);
        return Result.fail(exception.getMessage());
    }

    @ExceptionHandler(value = BlogException.class)
    public Result exceptionHandler(BlogException exception) {
        log.error(exception.getMessage(), exception);
        return Result.fail(exception.getMessage());
    }

    @ExceptionHandler(value = HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result exceptionHandler(HandlerMethodValidationException exception) {
        log.error(exception.getMessage(), exception);
        return Result.fail("参数校验失败");
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result exceptionHandler(MethodArgumentNotValidException exception) {
        log.error(exception.getMessage(), exception);
        return Result.fail("参数校验失败");
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void exceptionHandler(NoResourceFoundException exception) {
    }
}
