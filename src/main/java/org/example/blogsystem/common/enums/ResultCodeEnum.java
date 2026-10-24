package org.example.blogsystem.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码
 * <p>
 * 约定：
 * <ul>
 *   <li>200：成功</li>
 *   <li>1xxx：参数与校验类</li>
 *   <li>1002~1004：用户与登录类</li>
 *   <li>1005：博客类</li>
 *   <li>1006~1007：权限与登录态类</li>
 *   <li>1029：频率限制（沿用 HTTP 429 的语义，便于识别）</li>
 * </ul>
 * 说明：HTTP 状态码保持原有语义（200/400/401），业务码只用于前端区分提示文案；
 * 之前所有失败都返回 code=-1，前端无法区分"用户不存在"和"密码错误"。
 */
@Getter
@AllArgsConstructor
public enum ResultCodeEnum {
    SUCCESS(200),
    /** 兼容历史：未细分的失败 */
    FAIL(-1),

    PARAM_ERROR(1001),
    USER_NOT_EXIST(1002),
    PASSWORD_ERROR(1003),
    USERNAME_EXISTS(1004),
    BLOG_NOT_FOUND(1005),
    NO_PERMISSION(1006),
    NOT_LOGIN(1007),
    TOO_MANY_REQUESTS(1029);

    private Integer code;
}
