package com.example.blogcloud.common.constant;

/**
 * 项目通用常量
 */
public class Constants {
    /** 前端携带 token 的请求头 */
    public static final String TOKEN = "user_token";
    /** 兼容旧字段名的请求头 */
    public static final String TOKEN_OLD = "userToken";

    /** 网关校验通过后, 注入到下游服务请求头的用户 id / 用户名 */
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
}
