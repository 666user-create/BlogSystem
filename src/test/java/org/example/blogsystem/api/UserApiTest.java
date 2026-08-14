package org.example.blogsystem.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 用户模块接口测试
 * ============================================================
 * 覆盖：注册 /user/register、登录 /user/login、
 *       查询用户信息 /user/getUserInfo、根据博客查作者 /user/getAuthorInfo
 *
 * 对应测试用例文档：docs/03-测试用例设计.md 的 TC-REG / TC-LOG / TC-UINF
 * ============================================================
 */
class UserApiTest extends BaseApiTest {

    // ==================== 注册 ====================

    @Test
    @DisplayName("TC-REG-01 合法信息注册成功")
    void register_success() {
        // 用随机用户名，保证每次运行不冲突
        String userName = randomUserName("reg");

        Response response = given()
                .contentType("application/json")
                .body("{\"userName\":\"" + userName + "\","
                        + "\"password\":\"123456\","
                        + "\"confirmPassword\":\"123456\"}")
        .when()
                .post("/user/register");

        // 统一响应结构：{"code":200,"data":"注册成功","errMsg":null}
        response.then()
                .statusCode(200)                     // HTTP 状态码
                .body("code", equalTo(200))          // 业务码：成功
                .body("data", equalTo("注册成功"));  // 业务数据：成功文案
    }

    @Test
    @DisplayName("TC-REG-07 用户名已存在注册失败")
    void register_duplicateUserName_fail() {
        // 先注册一个用户，再重复注册同一个用户名
        String userName = registerAndLogin("dup");
        given()
                .contentType("application/json")
                .body("{\"userName\":\"" + userName + "\","
                        + "\"password\":\"123456\","
                        + "\"confirmPassword\":\"123456\"}")
        .when()
                .post("/user/register")
        .then()
                .statusCode(200)                                     // 业务失败时 HTTP 仍是 200
                .body("code", equalTo(-1))                           // 业务码：失败
                .body("errMsg", equalTo("用户名已被注册"));          // 错误信息
    }

    @Test
    @DisplayName("TC-REG-09 用户名/密码为空时参数校验失败(400)")
    void register_emptyParam_badRequest() {
        // 缺 userName 字段 → 触发 @NotNull 校验 → HTTP 400
        given()
                .contentType("application/json")
                .body("{\"password\":\"123456\",\"confirmPassword\":\"123456\"}")
        .when()
                .post("/user/register")
        .then()
                .statusCode(400)                                  // 参数校验失败返回 400
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("参数校验失败"));
    }

    @Test
    @DisplayName("TC-REG-03 用户名过短(3字符)参数校验失败")
    void register_userNameTooShort_badRequest() {
        given()
                .contentType("application/json")
                .body("{\"userName\":\"abc\",\"password\":\"123456\",\"confirmPassword\":\"123456\"}")
        .when()
                .post("/user/register")
        .then()
                .statusCode(400)
                .body("errMsg", equalTo("参数校验失败"));
    }

    // ==================== 登录 ====================

    @Test
    @DisplayName("TC-LOG-01 正确账号密码登录成功，返回 token")
    void login_success() {
        String userName = registerAndLogin("login");

        Response response = given()
                .contentType("application/json")
                .body("{\"userName\":\"" + userName + "\",\"password\":\"123456\"}")
        .when()
                .post("/user/login");

        // 登录成功响应：{"code":200,"data":{"userId":..,"token":"..","userName":".."}}
        response.then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.userId", notNullValue())     // 用户 id 不为空
                .body("data.token", notNullValue())      // token 不为空
                .body("data.userName", equalTo(userName));
    }

    @Test
    @DisplayName("TC-LOG-02 用户名不存在登录失败")
    void login_userNotExist_fail() {
        given()
                .contentType("application/json")
                .body("{\"userName\":\"no_such_user_000\",\"password\":\"123456\"}")
        .when()
                .post("/user/login")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("用户不存在"));
    }

    @Test
    @DisplayName("TC-LOG-03 密码错误登录失败")
    void login_wrongPassword_fail() {
        String userName = registerAndLogin("pwd");
        given()
                .contentType("application/json")
                .body("{\"userName\":\"" + userName + "\",\"password\":\"wrong-pass\"}")
        .when()
                .post("/user/login")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("密码错误"));
    }

    // ==================== 用户信息 ====================

    @Test
    @DisplayName("TC-UINF-01 查询存在的用户信息(含博客数)")
    void getUserInfo_success() {
        // 登录拿到 userId（token 和 userId 都在登录响应里）
        String userName = registerAndLogin("info");
        Response loginResp = given()
                .contentType("application/json")
                .body("{\"userName\":\"" + userName + "\",\"password\":\"123456\"}")
        .when()
                .post("/user/login");
        int userId = loginResp.jsonPath().getInt("data.userId");

        // 查询该用户信息（需要携带 token：请求头 user_token）
        given()
                .header("user_token", userAToken)
        .when()
                .get("/user/getUserInfo?userId=" + userId)
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.userName", equalTo(userName))
                .body("data.blogCount", notNullValue());   // 博客数不为空(可能为0)
    }

    @Test
    @DisplayName("TC-UINF-02 查询不存在的用户返回空数据")
    void getUserInfo_notExist_returnsEmpty() {
        given()
                .header("user_token", userAToken)
        .when()
                .get("/user/getUserInfo?userId=99999999")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", nullValue());   // 用户不存在时 data 为 null(不报错)
    }

    @Test
    @DisplayName("TC-UINF-06 根据不存在的博客查作者返回错误")
    void getAuthorInfo_blogNotExist_fail() {
        given()
                .header("user_token", userAToken)
        .when()
                .get("/user/getAuthorInfo?blogId=99999999")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("博客不存在"));
    }
}
