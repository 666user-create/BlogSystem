package org.example.blogsystem.common.pojo.response;

import lombok.Data;

@Data
public class Result {
    private Integer code;
    private String errMsg;
    private Object data;

    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(200);
        result.setData(data);
        return result;
    }

    public static Result fail(String errMsg) {
        Result result = new Result();
        result.setCode(-1);
        result.setErrMsg(errMsg);
        return result;
    }

    /** 带业务错误码的失败响应（前端可据此区分提示） */
    public static Result fail(Integer code, String errMsg) {
        Result result = new Result();
        result.setCode(code);
        result.setErrMsg(errMsg);
        return result;
    }

    public static Result fail(Object data) {
        Result result = new Result();
        result.setCode(-1);
        result.setData(data);
        return result;
    }
}
