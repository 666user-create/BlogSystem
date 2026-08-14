package org.example.blogsystem.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 博客模块接口测试
 * ============================================================
 * 覆盖：列表 /blog/getList、详情 /blog/getBlogDetail、
 *       新增 /blog/add、更新 /blog/update、删除 /blog/delete
 *
 * 对应测试用例文档：docs/03-测试用例设计.md 的 TC-LIST / TC-DET / TC-ADD / TC-UPD / TC-DEL
 *
 * 依赖的账号（来自基类 setUp）：
 *   - userA：作者本人，正常操作
 *   - userB：其他用户，用于"越权"测试
 * ============================================================
 */
class BlogApiTest extends BaseApiTest {

    // ==================== 列表 ====================

    @Test
    @DisplayName("TC-LIST-01 列表返回已上架博客数组")
    void getList_success() {
        // 先发一篇博客，确保列表里有数据
        addBlog(userAToken, "列表测试博客", "列表测试内容");

        Response response = given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getList");

        response.then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", notNullValue());   // data 是博客数组
    }

    // ==================== 详情 ====================

    @Test
    @DisplayName("TC-DET-01 博客详情正常返回")
    void getBlogDetail_success() {
        int blogId = addBlog(userAToken, "详情测试博客", "详情测试内容");

        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getBlogDetail?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data.title", equalTo("详情测试博客"))
                .body("data.content", equalTo("详情测试内容"));
    }

    @Test
    @DisplayName("TC-DET-02 不存在的博客返回'博客不存在'")
    void getBlogDetail_notExist_fail() {
        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getBlogDetail?blogId=99999999")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("博客不存在"));
    }

    @Test
    @DisplayName("TC-DET-04 blogId=0 参数校验失败(400)")
    void getBlogDetail_zeroId_badRequest() {
        // @Min(value = 1) 校验：blogId 必须大于 0
        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getBlogDetail?blogId=0")
        .then()
                .statusCode(400)
                .body("errMsg", equalTo("参数校验失败"));
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("TC-ADD-01 登录用户正常发表博客")
    void addBlog_success() {
        Response response = given()
                .header("user_token", userAToken)
                .contentType("application/json")
                .body("{\"title\":\"新增测试博客\",\"content\":\"新增测试内容\"}")
        .when()
                .post("/blog/add");

        // 新增成功返回布尔 true（被统一包装为 {"code":200,"data":true}）
        response.then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", equalTo(true));
    }

    @Test
    @DisplayName("TC-ADD-02 标题为空参数校验失败(400)")
    void addBlog_emptyTitle_badRequest() {
        given()
                .header("user_token", userAToken)
                .contentType("application/json")
                .body("{\"title\":\"\",\"content\":\"内容\"}")
        .when()
                .post("/blog/add")
        .then()
                .statusCode(400)
                .body("errMsg", equalTo("参数校验失败"));
    }

    @Test
    @DisplayName("TC-ADD-05 标题超过200字符参数校验失败(400)")
    void addBlog_titleTooLong_badRequest() {
        String longTitle = "长".repeat(201);   // 201 个字符，超过 @Size(max=200)

        given()
                .header("user_token", userAToken)
                .contentType("application/json")
                .body("{\"title\":\"" + longTitle + "\",\"content\":\"内容\"}")
        .when()
                .post("/blog/add")
        .then()
                .statusCode(400)
                .body("errMsg", equalTo("参数校验失败"));
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("TC-UPD-01 作者本人更新博客成功")
    void updateBlog_owner_success() {
        int blogId = addBlog(userAToken, "更新前标题", "更新前内容");

        // 作者 userA 更新自己的博客
        given()
                .header("user_token", userAToken)
                .contentType("application/json")
                .body("{\"id\":" + blogId + ",\"title\":\"更新后标题\",\"content\":\"更新后内容\"}")
        .when()
                .post("/blog/update")
        .then()
                .statusCode(200)
                .body("code", equalTo(200))
                .body("data", equalTo(true));

        // 验证：详情里能看到新标题
        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getBlogDetail?blogId=" + blogId)
        .then()
                .body("data.title", equalTo("更新后标题"));
    }

    @Test
    @DisplayName("TC-UPD-02 非作者更新他人博客被拒绝")
    void updateBlog_notOwner_fail() {
        int blogId = addBlog(userAToken, "A的博客", "A的内容");

        // 用 userB 的 token 去更新 userA 的博客 → 应被拒绝
        given()
                .header("user_token", userBToken)
                .contentType("application/json")
                .body("{\"id\":" + blogId + ",\"title\":\"B想篡改\",\"content\":\"B的内容\"}")
        .when()
                .post("/blog/update")
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("无权编辑该博客"));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("TC-DEL-01 作者删除博客后详情查不到(逻辑删除)")
    void deleteBlog_owner_success() {
        int blogId = addBlog(userAToken, "待删除博客", "待删除内容");

        // 作者删除
        given()
                .header("user_token", userAToken)
        .when()
                .post("/blog/delete?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("data", equalTo(true));

        // 验证：删除后详情返回"博客不存在"
        given()
                .header("user_token", userAToken)
        .when()
                .get("/blog/getBlogDetail?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("博客不存在"));
    }

    @Test
    @DisplayName("TC-DEL-02 非作者删除他人博客被拒绝")
    void deleteBlog_notOwner_fail() {
        int blogId = addBlog(userAToken, "A的博客2", "A的内容2");

        given()
                .header("user_token", userBToken)
        .when()
                .post("/blog/delete?blogId=" + blogId)
        .then()
                .statusCode(200)
                .body("code", equalTo(-1))
                .body("errMsg", equalTo("无权删除该博客"));
    }

    // ====================================================================
    // 工具方法（本类私有）
    // ====================================================================

    /**
     * 用指定 token 发表一篇博客，返回这篇博客的 id
     *
     * @param token   用户的登录 token
     * @param title   博客标题
     * @param content 博客内容
     * @return 新博客的 id（列表按 id 倒序，data[0] 即刚发的这篇）
     */
    private int addBlog(String token, String title, String content) {
        given()
                .header("user_token", token)
                .contentType("application/json")
                .body("{\"title\":\"" + title + "\",\"content\":\"" + content + "\"}")
        .when()
                .post("/blog/add")
        .then()
                .statusCode(200)
                .body("code", equalTo(200));

        // 新增接口只返回 true 不返回 id，从列表取最新一条（getList 按 id 倒序，data[0] 最新）
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
