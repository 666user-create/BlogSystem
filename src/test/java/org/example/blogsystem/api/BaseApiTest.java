package org.example.blogsystem.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

/**
 * 接口自动化测试基类
 * ============================================================
 * 这个类是所有接口测试（UserApiTest / BlogApiTest / AdminApiTest / AuthApiTest）的父类。
 * 它负责三件"准备工作"，让子类写用例时只需要关注"测什么"：
 *
 *   1. 启动整个 Spring Boot 应用（@SpringBootTest）
 *      - webEnvironment = RANDOM_PORT：给应用分配一个随机端口，
 *        避免和开发时手动启动的 8080 冲突。
 *      - 测试跑完应用自动关闭，无需手动启停。
 *
 *   2. 告诉 RestAssured "往哪里发请求"（baseURI / port）
 *      - RestAssured 是一个发 HTTP 请求的 Java 库，用法像 Postman，
 *        但它写在代码里，可以断言、可以进 CI。
 *
 *   3. 提前登录好三个账号，把 token 存起来供子类使用
 *      - admin（管理员）：测管理端接口
 *      - userA（普通用户 A）：测正常操作
 *      - userB（普通用户 B）：测越权操作（B 去改/删 A 的博客）
 *
 * 注意：
 *   - @ActiveProfiles("test")：使用 src/test/resources/application-test.yml
 *     里的测试数据库配置，与开发/生产配置隔离。
 *   - 需要本机 MySQL 已启动（连接配置见 application-test.yml）。
 * ============================================================
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseApiTest {

    /**
     * Spring 启动时会把随机端口注入到这个字段（来自 @LocalServerPort）
     */
    @LocalServerPort
    protected int port;

    /** 管理员 token（登录 admin/admin123 获得） */
    protected String adminToken;

    /** 普通用户 A 的 token，用于正常操作 */
    protected String userAToken;

    /** 普通用户 B 的 token，用于"非作者越权"测试 */
    protected String userBToken;

    /** 每个测试方法执行前都会先跑这里（@BeforeEach） */
    @BeforeEach
    void setUp() {
        // 1. 告诉 RestAssured 应用地址和随机端口
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;

        // 2. 登录三个账号，拿到 token
        //    （用户名带随机后缀，保证每次运行数据独立、可重复执行；
        //     注册密码固定为 123456，所以登录时也传 123456）
        adminToken = login("admin", "admin123");
        userAToken = login(registerAndLogin("userA"), "123456");
        userBToken = login(registerAndLogin("userB"), "123456");
    }

    // ====================================================================
    // 下面的方法都是"工具方法"：子类测试里直接调用，不用重复写 HTTP 细节
    // ====================================================================

    /**
     * 注册一个随机用户并返回其用户名（用于每次测试创建新用户，避免数据冲突）
     *
     * @param prefix 用户名前缀，例如 "userA"
     * @return 完整用户名，例如 "userA1756801234567"
     */
    protected String registerAndLogin(String prefix) {
        String userName = prefix + System.currentTimeMillis();
        // 注册请求体（JSON 字符串）
        String body = "{\"userName\":\"" + userName + "\","
                + "\"password\":\"123456\","
                + "\"confirmPassword\":\"123456\"}";

        // given()...when()...then() 是 RestAssured 的链式写法：
        // given()  = 准备请求（头、参数、body）
        // when()   = 发送请求（get/post/put/delete）
        // then()   = 校验响应（状态码、字段）
        given()
                .contentType("application/json")   // 告诉服务器 body 是 JSON
                .body(body)                        // 请求体内容
        .when()
                .post("/user/register")            // 发 POST 请求
        .then()
                .statusCode(200);                  // 期望 HTTP 200

        return userName;
    }

    /**
     * 登录并返回 token
     *
     * @param userName 用户名
     * @param password 密码
     * @return 登录成功返回的 token 字符串
     */
    protected String login(String userName, String password) {
        String body = "{\"userName\":\"" + userName + "\",\"password\":\"" + password + "\"}";

        Response response = given()
                .contentType("application/json")
                .body(body)
        .when()
                .post("/user/login");

        // 断言 HTTP 状态码为 200
        response.then().statusCode(200);

        // jsonPath() 用来从 JSON 响应里取值，类似 Postman 里的 pm.response.json()
        // 登录响应的结构：{"code":200,"data":{"userId":1,"token":"xxx","userName":"admin"}}
        return response.jsonPath().getString("data.token");
    }

    /**
     * 生成一个随机的唯一用户名（供注册使用）
     */
    protected String randomUserName(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}
