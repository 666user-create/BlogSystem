package com.example.blogcloud.common.pojo.response;

import lombok.Data;

/**
 * 统一响应结构
 */
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

    public static Result fail(Object data) {
        Result result = new Result();
        result.setCode(-1);
        result.setData(data);
        return result;
    }
}
