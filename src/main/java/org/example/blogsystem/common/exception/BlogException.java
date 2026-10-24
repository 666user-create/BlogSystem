package org.example.blogsystem.common.exception;

import org.example.blogsystem.common.enums.ResultCodeEnum;

/**
 * 业务异常：携带业务错误码 + 提示文案
 * <p>
 * 改造前只有 message，全局异常处理统一返回 code=-1，前端无法区分是"用户不存在"还是"密码错误"。
 * 现在由抛异常的地方指定 {@link ResultCodeEnum}，全局异常处理原样透传给前端。
 */
public class BlogException extends RuntimeException {

    private final Integer code;

    /** 兼容旧写法：不指定业务码时退回 FAIL(-1) */
    public BlogException(String message) {
        super(message);
        this.code = ResultCodeEnum.FAIL.getCode();
    }

    /** 推荐写法：指定业务错误码 */
    public BlogException(ResultCodeEnum resultCode, String message) {
        super(message);
        this.code = resultCode == null ? ResultCodeEnum.FAIL.getCode() : resultCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
