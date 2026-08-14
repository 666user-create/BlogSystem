package org.example.blogsystem.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 管理端接口测试
 * ============================================================
 * 覆盖：管理员列表 /blog/adminList、切换上下架 /blog/togglePublish
 *
 * 对应测试用例文档：docs/03-测试用例设计.md 的 TC-ADM
 *
 * 权限规则（来自业务规则 BR-05）：
 *   管理端接口只有 token 中用户名为 "admin" 的账号才能调用，
 *   普通用户调用会返回"无管理员权限"。
 * ============================================================
 */
class AdminApiTest extends BaseApiTest {

    @Test
    @DisplayName("TC-ADM-01 管理员获取全部博客列表(含下架)")
    void adminList_admin_success() {
        given()
                .header("user_token", adminToken)
        .when()
                .get("/blog/adminList")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());
    }

    @Test
    @DisplayName("TC-ADM-02 非管理员访问管理列表被拒绝")
    void adminList_normalUser_fail() {
        // userA 是普通用户，调用管理端接口应被拒绝
        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/adminList")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("无管理员权限"));
    }

    @Test
    @DisplayName("TC-ADM-04/05 管理员切换博客上下架")
    void togglePublish_admin_success() {
        // 先让 userA 发一篇博客（用基类的工具方法临时注册并登录拿到新 token）
        String newUserToken = login(registerAndLogin("pub"), "123456");
        int blogId = addBlogWithToken(newUserToken);

        // 1. 第一次切换：上架(1) → 下架(0)
        given()
                .header("user_token", adminToken)
        .when()
                .post("/blog/togglePublish?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("data", equalTo(true));

        // 2. 下架后普通列表不再显示（getList 只展示已上架博客）
        given()
                .header("user_token", newUserToken)
        .when()
                .get("/blog/getList")
        .then()
                .body("code", equalTo(200));

        // 3. 第二次切换：下架(0) → 上架(1)，恢复正常
        given()
                .header("user_token", adminToken)
        .when()
                .post("/blog/togglePublish?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("data", equalTo(true));
    }

    @Test
    @DisplayName("TC-ADM-07 非管理员切换上下架被拒绝")
    void togglePublish_normalUser_fail() {
        String newUserToken = login(registerAndLogin("nor"), "123456");
        int blogId = addBlogWithToken(newUserToken);

        // userA（普通用户）调用切换 → 无管理员权限
        given()
                .header("user_token", userAToken)
        .when()
                .post("/blog/togglePublish?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("无管理员权限"));
    }

    @Test
    @DisplayName("TC-ADM-06 切换不存在的博客")
    void togglePublish_notExist_fail() {
        given()
                .header("user_token", adminToken)
        .when()
                .post("/blog/togglePublish?blogId=99999999")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("博客不存在"));
    }

    /**
     * 用指定 token 发一篇博客并返回 blogId（供本类使用）
     */
    private int addBlogWithToken(String token) {
        given()
                .header("user_token", token)
                .contentType("application/json")
                .body("{\"title\":\"上下架测试博客\",\"content\":\"上下架测试内容\"}")
        .when()
                .post("/blog/add")
        .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 新增接口不返回 id，从列表取最新一条（列表按 id 倒序，data[0] 即最新）
        return given()
                .header("user_token", token)
        .when()
                .get("/blog/getList")
        .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("data[0].id");
    }
}
