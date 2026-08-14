package org.example.blogsystem.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * 鉴权模块接口测试
 * ============================================================
 * 覆盖：JWT 登录拦截器对受保护接口的校验（业务规则 BR-01）
 *
 * 拦截规则：除 /user/login、/user/register 外，
 * 所有 /user/**、/blog/** 接口必须携带有效 token，
 * token 缺失/无效/过期 → HTTP 401（无响应体，拦截器直接返回）
 *
 * 对应测试用例文档：docs/03-测试用例设计.md 的 TC-AUTH
 * ============================================================
 */
class AuthApiTest extends BaseApiTest {

    @Test
    @DisplayName("TC-AUTH-01 无 token 访问受保护接口返回 401")
    void noToken_unauthorized() {
        given()
        .when()
                .get("/blog/getList")          // 不带任何 token 请求头
        .then()
                .statusCode(401);              // 期望 401 未登录
    }

    @Test
    @DisplayName("TC-AUTH-04 空字符串 token 返回 401")
    void emptyToken_unauthorized() {
        given()
                .header("user_token", "")      // token 为空字符串
        .when()
                .get("/blog/getList")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("TC-AUTH-02 篡改 token 返回 401")
    void tamperedToken_unauthorized() {
        // 取一个有效 token，篡改末尾字符破坏签名
        String validToken = userAToken;
        String tampered = validToken.substring(0, validToken.length() - 4) + "xxxx";

        given()
                .header("user_token", tampered)
        .when()
                .get("/blog/getList")
        .then()
                .statusCode(401);              // JWT 签名校验失败 → 401
    }

    @Test
    @DisplayName("TC-AUTH-05 登录/注册接口无需 token 可访问")
    void loginRegister_noTokenAllowed() {
        // 登录接口不带 token → 200（返回"用户不存在"，说明请求到达了业务层而非被拦截）
        given()
                .contentType("application/json")
                .body("{\"userName\":\"no_such_user\",\"password\":\"123456\"}")
        .when()
                .post("/user/login")
        .then()
                .statusCode(200);

        // 注册接口不带 token → 200
        given()
                .contentType("application/json")
                .body("{\"userName\":\"" + randomUserName("auth") + "\","
                        + "\"password\":\"123456\",\"confirmPassword\":\"123456\"}")
        .when()
                .post("/user/register")
        .then()
                .statusCode(200);
    }
}
