package org.example.blogsystem.ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 博客系统 UI 自动化测试（Selenium WebDriver + JUnit5）
 * ============================================================
 * 测试对象：前端静态页面（登录/注册/列表/编辑/详情/更新）
 *
 * 设计要点（对应企业实践）：
 *   1. @SpringBootTest(RANDOM_PORT)：测试自己启动应用，不依赖手工启动的 8080；
 *   2. Headless Chrome：无界面运行，适合 CI；
 *   3. 显式等待（WebDriverWait）：不用 Thread.sleep，等待元素/跳转/alert，降低偶发失败；
 *   4. 截图断言：关键步骤截图保存到 docs/test-evidence/ui/，并断言文件生成成功；
 *   5. 数据独立：每次注册随机用户名，用例可重复执行。
 *
 * 运行：mvn test -Dtest=BlogUiTest（需要本机 MySQL，用户名/密码见 application-test.yml）
 * ============================================================
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BlogUiTest {

    /** 显式等待超时（页面渲染 + AJAX 请求） */
    private static final Duration WAIT = Duration.ofSeconds(15);

    /** 截图证据目录（相对项目根，随 git 提交） */
    private static final String SHOT_DIR = "docs/test-evidence/ui";

    private static WebDriver driver;

    @LocalServerPort
    private int port;

    private String baseUrl;

    @BeforeAll
    static void startBrowser() throws IOException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1440,1000");
        driver = new ChromeDriver(options);
        Files.createDirectories(Paths.get(SHOT_DIR));
    }

    @AfterAll
    static void quitBrowser() throws IOException {
        // 弹框文本证据落盘：原生 alert/confirm 无法截图，用文本证据补齐
        Files.write(Paths.get(SHOT_DIR, "alert-evidence.md"),
                ("# UI 弹框提示证据\n\n"
                        + "> 浏览器原生 alert/confirm 不属于页面 DOM，Selenium 截图无法捕获弹窗内容。\n"
                        + "> 截图中右上角的黄色浮层为测试脚本在关闭弹窗后按实际文本绘制的提示，\n"
                        + "> 完整文本证据如下（按用例执行顺序）：\n\n"
                        + String.join("\n", alertEvidence) + "\n").getBytes(StandardCharsets.UTF_8));
        if (driver != null) {
            driver.quit();
        }
    }

    /** 每个用例从"未登录的登录页"开始，清掉上个用例残留的登录态 */
    @BeforeEach
    void setUp() {
        baseUrl = "http://127.0.0.1:" + port;
        driver.get(baseUrl + "/blog_login.html");
        ((JavascriptExecutor) driver).executeScript("sessionStorage.clear()");
    }

    // ==================================================================
    // 工具方法
    // ==================================================================

    /** 截图并做"截图断言"：文件必须生成且非空 */
    private void screenshot(String name) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path dest = Paths.get(SHOT_DIR, name + ".png");
            Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            assertTrue(Files.exists(dest) && Files.size(dest) > 0, "截图应生成且非空: " + name);
        } catch (IOException e) {
            fail("截图失败: " + e.getMessage());
        }
    }

    /** 弹框文本证据（用例执行过程中累积，全部执行完落盘为文件） */
    private static final List<String> alertEvidence = new ArrayList<>();

    /**
     * 等待 alert 弹出、取文本并关闭。
     * <p>
     * 注意：浏览器原生 alert 不属于页面 DOM，Selenium 截图【无法】捕获弹窗内容，
     * 因此这里在关闭弹窗后把实际提示文本绘制成页面浮层，使截图能体现"究竟提示了什么"；
     * 同时把文本累积到 alertEvidence，全部用例结束后落盘为文本证据文件。
     *
     * @param scene 场景描述（用于证据文件可读性）
     */
    private String acceptAlert(String scene) {
        Alert alert = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.alertIsPresent());
        String text = alert.getText();
        alert.accept();

        alertEvidence.add("- " + scene + " → 弹框提示：" + text);

        // 把提示内容绘制到页面上，供截图留证（页面跳转后浮层消失，故同时保留文本证据）
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var d=document.createElement('div');"
                            + "d.style.cssText='position:fixed;top:16px;right:16px;z-index:99999;"
                            + "background:#fff8e1;border:2px solid #ffb300;border-radius:6px;"
                            + "padding:10px 16px;font-size:15px;font-family:sans-serif;color:#333;"
                            + "box-shadow:0 2px 10px rgba(0,0,0,.25)';"
                            + "d.textContent='弹框提示：' + arguments[0];"
                            + "document.body.appendChild(d);", text);
        } catch (Exception ignored) {
            // 页面已跳转时插入可能失败，不影响断言
        }
        return text;
    }

    /** 注册一个随机用户并返回用户名（不登录） */
    private String register(String prefix) {
        String userName = prefix + (System.currentTimeMillis() % 1000000);
        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.id("confirmPassword")).sendKeys("123456");
        driver.findElement(By.id("submit")).click();
        acceptAlert("注册提交（成功）");
        return userName;
    }

    /** 注册新用户并登录，返回用户名（后续用例可以直接用这个登录态） */
    private String registerAndLogin(String prefix) {
        String userName = register(prefix);
        loginExpectSuccess(userName, "123456");
        return userName;
    }

    /** 登录并期望成功（跳转到列表页） */
    private void loginExpectSuccess(String userName, String password) {
        driver.get(baseUrl + "/blog_login.html");
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("submit")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_list.html"));
    }

    /** 登录并返回 alert 文本（用于失败场景） */
    private String loginExpectAlert(String userName, String password) {
        driver.get(baseUrl + "/blog_login.html");
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("submit")).click();
        return acceptAlert("登录提交（失败场景）");
    }

    /** 等待 editor.md 编辑器初始化完成（editor.md 异步挂载底层 CodeMirror，必须等 cm 就绪） */
    private void waitEditorReady() {
        new WebDriverWait(driver, WAIT).until(d -> Boolean.TRUE.equals(
                ((JavascriptExecutor) d).executeScript(
                        "return !!(window.editor && window.editor.cm && window.editor.cm.setValue)")));
    }

    /** 通过底层 CodeMirror 实例写入 Markdown 内容（比 setMarkdown 更稳，避免内部时序问题） */
    private void setEditorContent(String content) {
        ((JavascriptExecutor) driver).executeScript("window.editor.cm.setValue(arguments[0])", content);
    }

    /** 发表一篇博客（需已登录），成功后停在列表页 */
    private void publishBlog(String title, String content) {
        driver.get(baseUrl + "/blog_edit.html");
        waitEditorReady();
        driver.findElement(By.id("title")).sendKeys(title);
        setEditorContent(content);
        driver.findElement(By.id("submit")).click();
        acceptAlert("发表博客");
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_list.html"));
    }

    /** 打开列表页第一篇博客的详情，返回 blogId */
    private String openFirstBlogDetail() {
        driver.get(baseUrl + "/blog_list.html");
        WebElement link = new WebDriverWait(driver, WAIT).until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".container .right .blog .detail")));
        String href = link.getAttribute("href");
        String blogId = href.substring(href.indexOf("blogId=") + "blogId=".length());
        link.click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_detail.html"));
        return blogId;
    }

    /** 列表页所有博客标题 */
    private List<String> listBlogTitles() {
        return driver.findElements(By.cssSelector(".container .right .blog .title"))
                .stream().map(WebElement::getText).toList();
    }

    // ==================================================================
    // 一、登录模块
    // ==================================================================

    @Test
    @Order(1)
    @DisplayName("UI-01 登录页元素展示正确")
    void ui01_loginPageElements() {
        driver.get(baseUrl + "/blog_login.html");

        assertTrue(driver.findElement(By.id("username")).isDisplayed(), "用户名输入框应展示");
        assertTrue(driver.findElement(By.id("password")).isDisplayed(), "密码输入框应展示");
        assertTrue(driver.findElement(By.id("submit")).isDisplayed(), "提交按钮应展示");
        assertTrue(driver.getTitle().contains("登陆"), "页面标题应为登陆页");

        screenshot("UI-01-登录页元素展示");
    }

    @Test
    @Order(2)
    @DisplayName("UI-02 用户名密码为空时前端拦截提示")
    void ui02_loginBlankInput() {
        driver.get(baseUrl + "/blog_login.html");
        driver.findElement(By.id("submit")).click();

        String alert = acceptAlert("UI-02 空输入登录");
        assertEquals("用户名和密码不能为空", alert);
        screenshot("UI-02-空输入登录提示");
    }

    @Test
    @Order(3)
    @DisplayName("UI-03 用户名不存在登录失败提示")
    void ui03_loginUserNotExist() {
        String alert = loginExpectAlert("no_such_user_99999", "123456");

        assertEquals("用户不存在", alert);
        screenshot("UI-03-用户不存在提示");
    }

    @Test
    @Order(4)
    @DisplayName("UI-04 密码错误登录失败提示")
    void ui04_loginWrongPassword() {
        String userName = register("pwd");
        String alert = loginExpectAlert(userName, "wrong123");

        assertEquals("密码错误", alert);
        screenshot("UI-04-密码错误提示");
    }

    @Test
    @Order(5)
    @DisplayName("UI-05 正确账号登录成功并跳转列表页")
    void ui05_loginSuccess() {
        String userName = register("ok");
        loginExpectSuccess(userName, "123456");

        assertTrue(driver.getCurrentUrl().contains("blog_list.html"), "登录成功应跳转列表页");
        screenshot("UI-05-登录成功跳转列表页");
    }

    @Test
    @Order(6)
    @DisplayName("UI-06 登录后列表页左侧展示当前用户信息")
    void ui06_listPageShowsCurrentUser() {
        String userName = registerAndLogin("me");
        driver.get(baseUrl + "/blog_list.html");

        WebElement card = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".container .left .card h3")));
        assertEquals(userName, card.getText(), "左侧卡片应展示当前登录用户名");
        screenshot("UI-06-用户信息展示");
    }

    // ==================================================================
    // 二、注册模块
    // ==================================================================

    @Test
    @Order(7)
    @DisplayName("UI-07 注册页元素展示正确")
    void ui07_registerPageElements() {
        driver.get(baseUrl + "/blog_register.html");

        assertTrue(driver.findElement(By.id("username")).isDisplayed());
        assertTrue(driver.findElement(By.id("password")).isDisplayed());
        assertTrue(driver.findElement(By.id("confirmPassword")).isDisplayed());
        assertTrue(driver.findElement(By.id("githubUrl")).isDisplayed());
        screenshot("UI-07-注册页元素展示");
    }

    @Test
    @Order(8)
    @DisplayName("UI-08 注册用户名过短被前端拦截")
    void ui08_registerUserNameTooShort() {
        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys("abc");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.id("confirmPassword")).sendKeys("123456");
        driver.findElement(By.id("submit")).click();

        assertEquals("用户名长度必须在4到20位之间", acceptAlert("UI-08 用户名过短"));
        screenshot("UI-08-用户名过短提示");
    }

    @Test
    @Order(9)
    @DisplayName("UI-09 注册密码过短被前端拦截")
    void ui09_registerPasswordTooShort() {
        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys("abcd1234");
        driver.findElement(By.id("password")).sendKeys("123");
        driver.findElement(By.id("confirmPassword")).sendKeys("123");
        driver.findElement(By.id("submit")).click();

        assertEquals("密码长度必须在6到20位之间", acceptAlert("UI-09 密码过短"));
        screenshot("UI-09-密码过短提示");
    }

    @Test
    @Order(10)
    @DisplayName("UI-10 注册两次密码不一致被前端拦截")
    void ui10_registerPasswordMismatch() {
        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys("abcde12345");
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.id("confirmPassword")).sendKeys("654321");
        driver.findElement(By.id("submit")).click();

        assertEquals("两次输入的密码不一致", acceptAlert("UI-10 密码不一致"));
        screenshot("UI-10-密码不一致提示");
    }

    @Test
    @Order(11)
    @DisplayName("UI-11 注册成功后跳转登录页")
    void ui11_registerSuccess() {
        String userName = "reg" + (System.currentTimeMillis() % 1000000);

        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.id("confirmPassword")).sendKeys("123456");
        driver.findElement(By.id("submit")).click();

        assertEquals("注册成功，请登录", acceptAlert("UI-11 注册成功"));
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_login.html"));
        screenshot("UI-11-注册成功跳转");

        // 注册完能正常登录，形成闭环
        loginExpectSuccess(userName, "123456");
        assertTrue(driver.getCurrentUrl().contains("blog_list.html"));
    }

    @Test
    @Order(12)
    @DisplayName("UI-12 重复用户名注册被拦截")
    void ui12_registerDuplicateUserName() {
        String userName = register("dup");

        driver.get(baseUrl + "/blog_register.html");
        driver.findElement(By.id("username")).sendKeys(userName);
        driver.findElement(By.id("password")).sendKeys("123456");
        driver.findElement(By.id("confirmPassword")).sendKeys("123456");
        driver.findElement(By.id("submit")).click();

        assertEquals("用户名已被注册", acceptAlert("UI-12 重复用户名"));
        screenshot("UI-12-重复用户名提示");
    }

    // ==================================================================
    // 三、博客列表与发布
    // ==================================================================

    @Test
    @Order(13)
    @DisplayName("UI-13 列表页展示博客卡片")
    void ui13_listShowsBlogCard() {
        String title = "UI列表测试博客" + (System.currentTimeMillis() % 100000);
        registerAndLogin("list");
        publishBlog(title, "这是 UI 自动化测试写入的内容");

        assertTrue(listBlogTitles().contains(title), "列表应包含刚发表的博客");
        screenshot("UI-13-列表展示博客卡片");
    }

    @Test
    @Order(14)
    @DisplayName("UI-14 列表页展示当前用户博客数")
    void ui14_listShowsBlogCount() {
        registerAndLogin("cnt");
        publishBlog("UI计数测试博客", "内容");

        driver.get(baseUrl + "/blog_list.html");
        WebElement count = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.id("blog-count")));
        assertEquals("1", count.getText(), "新用户发表 1 篇后博客数应为 1");
        screenshot("UI-14-博客数展示");
    }

    @Test
    @Order(15)
    @DisplayName("UI-15 写博客标题为空被前端拦截")
    void ui15_publishBlankTitle() {
        registerAndLogin("blank");

        driver.get(baseUrl + "/blog_edit.html");
        waitEditorReady();
        driver.findElement(By.id("submit")).click();

        assertEquals("标题不能为空", acceptAlert("UI-15 标题为空"));
        screenshot("UI-15-标题为空提示");
    }

    @Test
    @Order(16)
    @DisplayName("UI-16 发表博客成功并出现在列表")
    void ui16_publishSuccess() {
        String title = "UI发表成功" + (System.currentTimeMillis() % 100000);
        registerAndLogin("pub");

        publishBlog(title, "## 标题\n\n发表成功的内容");

        assertTrue(driver.getCurrentUrl().contains("blog_list.html"), "发表成功应回到列表页");
        assertTrue(listBlogTitles().contains(title), "新博客应出现在列表");
        screenshot("UI-16-发表成功");
    }

    // ==================================================================
    // 四、博客详情 / 编辑 / 删除
    // ==================================================================

    @Test
    @Order(17)
    @DisplayName("UI-17 详情页展示博客标题与内容")
    void ui17_detailShowsTitleAndContent() {
        String title = "UI详情测试" + (System.currentTimeMillis() % 100000);
        registerAndLogin("det");
        publishBlog(title, "详情页内容校验文本");
        openFirstBlogDetail();

        WebElement detailTitle = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .title")));
        assertEquals(title, detailTitle.getText());
        assertTrue(driver.findElement(By.cssSelector(".content .detail")).getText().contains("详情页内容校验文本"));
        screenshot("UI-17-详情页展示");
    }

    @Test
    @Order(18)
    @DisplayName("UI-18 详情页展示作者信息")
    void ui18_detailShowsAuthor() {
        String userName = registerAndLogin("author");
        publishBlog("UI作者信息测试", "内容");
        openFirstBlogDetail();

        WebElement author = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".container .left .card h3")));
        assertEquals(userName, author.getText(), "详情页左侧应展示博客作者");
        screenshot("UI-18-作者信息展示");
    }

    @Test
    @Order(19)
    @DisplayName("UI-19 编辑页正确回显原博客，并保存成功")
    void ui19_updateBlog() {
        String oldTitle = "UI编辑前" + (System.currentTimeMillis() % 100000);
        String newTitle = "UI编辑后" + (System.currentTimeMillis() % 100000);
        registerAndLogin("upd");
        publishBlog(oldTitle, "编辑前的内容");
        String blogId = openFirstBlogDetail();

        // 详情页点"编辑"进入更新页
        driver.findElement(By.xpath("//div[@class='operating']//button[text()='编辑']")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_update.html"));
        waitEditorReady();

        // ===== 关键断言（BUG-14 回归点）=====
        // 编辑页必须正确回显原博客内容：editor.md 异步初始化，
        // 若回显时机早于编辑器就绪，编辑框会显示默认占位文本，提交后原内容被覆盖。
        WebElement titleInput = driver.findElement(By.id("title"));
        new WebDriverWait(driver, WAIT).until(d ->
                !((WebElement) d.findElement(By.id("title"))).getAttribute("value").isEmpty());
        assertEquals(oldTitle, titleInput.getAttribute("value"), "编辑页应回显原博客标题");

        String echoedContent = (String) ((JavascriptExecutor) driver)
                .executeScript("return window.editor.getMarkdown()");
        assertTrue(echoedContent != null && echoedContent.contains("编辑前的内容"),
                "编辑页应回显原博客内容，实际回显: " + echoedContent);
        screenshot("UI-19a-编辑页回显原内容");

        // 修改标题与内容后提交
        titleInput.clear();
        titleInput.sendKeys(newTitle);
        setEditorContent("编辑后的内容");
        driver.findElement(By.id("submit")).click();

        assertEquals("更新成功", acceptAlert("UI-19 编辑保存"));
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_detail.html"));

        // 断言：详情页显示新标题、新内容
        WebElement detailTitle = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .title")));
        assertEquals(newTitle, detailTitle.getText(), "详情页应显示更新后的标题");
        assertTrue(driver.findElement(By.cssSelector(".content .detail")).getText().contains("编辑后的内容"));
        screenshot("UI-19b-编辑保存成功");

        // 清理：把这篇文章删掉，避免影响后续用例的"第一篇博客"断言
        deleteCurrentBlog(blogId);
    }

    @Test
    @Order(20)
    @DisplayName("UI-20 删除博客后列表不再展示")
    void ui20_deleteBlog() {
        String title = "UI待删除" + (System.currentTimeMillis() % 100000);
        registerAndLogin("del");
        publishBlog(title, "待删除的内容");
        String blogId = openFirstBlogDetail();

        deleteCurrentBlog(blogId);

        driver.get(baseUrl + "/blog_list.html");
        assertFalse(listBlogTitles().contains(title), "删除后列表不应再展示该博客");
        screenshot("UI-20-删除后列表");
    }

    /** 在详情页执行删除（处理 confirm 弹窗）并回到列表页 */
    private void deleteCurrentBlog(String blogId) {
        driver.get(baseUrl + "/blog_detail.html?blogId=" + blogId);
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class='operating']//button[text()='删除']"))).click();
        acceptAlert("删除确认");
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_list.html"));
    }

    // ==================================================================
    // 五、登录态与权限
    // ==================================================================

    @Test
    @Order(21)
    @DisplayName("UI-21 注销后回到登录页")
    void ui21_logout() {
        registerAndLogin("out");

        driver.findElement(By.xpath("//a[text()='注销']")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("blog_login.html"));
        screenshot("UI-21-注销回到登录页");
    }

    @Test
    @Order(22)
    @DisplayName("UI-22 普通用户不显示管理入口")
    void ui22_normalUserNoAdminEntry() {
        registerAndLogin("normal");
        driver.get(baseUrl + "/blog_list.html");
        // 等页面脚本执行完（列表区域渲染出来）
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".container .right")));
        driver.findElement(By.cssSelector(".container .left .card h3"));

        boolean adminVisible = driver.findElement(By.cssSelector(".nav-admin")).isDisplayed();
        assertFalse(adminVisible, "普通用户不应看到管理入口");
        screenshot("UI-22-普通用户无管理入口");
    }

    @Test
    @Order(23)
    @DisplayName("UI-23 管理员登录后显示管理入口")
    void ui23_adminSeesAdminEntry() {
        loginExpectSuccess("admin", "admin123");
        driver.get(baseUrl + "/blog_list.html");

        WebElement adminEntry = new WebDriverWait(driver, WAIT).until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".nav-admin")));
        assertTrue(adminEntry.isDisplayed(), "管理员应看到管理入口");
        screenshot("UI-23-管理员入口展示");
    }

    // ==================================================================
    // 六、页面数据展示正确性
    // 说明：前面的用例只保证"操作能成功"，这一组专门验证"显示的数据对不对"——
    //      这类问题（内容不回显、Markdown 不渲染、表头与数据不符）不会让操作失败，
    //      但会让用户看到错误内容，属于最容易漏测的场景。
    // ==================================================================

    @Test
    @Order(24)
    @DisplayName("UI-24 详情页正确渲染 Markdown 内容")
    void ui24_detailRendersMarkdown() {
        String markdown = "## 二级标题\n\n**加粗文本**";
        registerAndLogin("md");
        publishBlog("Markdown渲染测试博客", markdown);
        openFirstBlogDetail();

        WebElement detail = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .detail")));
        String innerHtml = detail.getAttribute("innerHTML");
        screenshot("UI-24-详情页Markdown渲染");

        assertTrue(innerHtml.contains("<h2") || innerHtml.contains("<strong"),
                "详情页应把 Markdown 渲染为 HTML（应出现 h2/strong 标签），实际 innerHTML: " + innerHtml);
    }

    @Test
    @Order(25)
    @DisplayName("UI-25 管理页表头列与数据字段一致")
    void ui25_adminTableHeaderMatchesData() {
        String title = "管理页字段一致性" + (System.currentTimeMillis() % 100000);
        registerAndLogin("admcol");
        publishBlog(title, "内容");

        loginExpectSuccess("admin", "admin123");
        driver.get(baseUrl + "/blog_admin.html");
        new WebDriverWait(driver, WAIT).until(d ->
                !d.findElement(By.id("blog-tbody")).getText().contains("加载中"));

        List<String> headers = driver.findElements(By.cssSelector(".admin-table thead th"))
                .stream().map(WebElement::getText).toList();
        WebElement row = driver.findElement(By.xpath(
                "//tbody[@id='blog-tbody']//tr[td[contains(text(),'" + title + "')]]"));
        List<String> cells = row.findElements(By.tagName("td")).stream().map(WebElement::getText).toList();
        screenshot("UI-25-管理页字段一致性");

        assertEquals(headers.size(), cells.size(), "表头列数与数据列数应一致");
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).contains("时间")) {
                assertFalse(cells.get(i) == null || cells.get(i).isBlank(),
                        "管理页『" + headers.get(i) + "』列应有数据，实际为空");
            }
        }
    }

    @Test
    @Order(26)
    @DisplayName("UI-26 列表页展示的标题与摘要与原文一致")
    void ui26_listDataMatchesContent() {
        String title = "列表数据一致性" + (System.currentTimeMillis() % 100000);
        registerAndLogin("listdata");
        publishBlog(title, "列表摘要应展示这段文字");

        driver.get(baseUrl + "/blog_list.html");
        WebElement card = new WebDriverWait(driver, WAIT).until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@class='blog'][div[@class='title' and contains(text(),'" + title + "')]]")));

        String cardTitle = card.findElement(By.className("title")).getText();
        String cardDesc = card.findElement(By.className("desc")).getText();
        screenshot("UI-26-列表数据一致性");

        assertEquals(title, cardTitle, "列表卡片标题应与原文一致");
        assertTrue(cardDesc.contains("列表摘要应展示这段文字"),
                "列表摘要应展示原文内容，实际: " + cardDesc);
    }

    // ==================================================================
    // 七、作者展示与操作权限
    // ==================================================================

    @Test
    @Order(27)
    @DisplayName("UI-27 详情页展示作者名")
    void ui27_detailShowsAuthorName() {
        String userName = registerAndLogin("authname");
        publishBlog("作者显示测试博客", "内容");
        openFirstBlogDetail();

        WebElement author = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .author")));
        screenshot("UI-27-详情页作者名");

        assertEquals("作者：" + userName, author.getText(), "详情页正文区应展示作者名");
    }

    @Test
    @Order(28)
    @DisplayName("UI-28 作者本人可见编辑/删除按钮")
    void ui28_ownerSeesOperatingButtons() {
        registerAndLogin("ownerbtn");
        publishBlog("作者可见按钮测试", "内容");
        openFirstBlogDetail();

        WebElement operating = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .operating")));
        screenshot("UI-28-作者可见编辑删除");

        assertTrue(operating.isDisplayed(), "作者本人应看到编辑/删除按钮");
    }

    @Test
    @Order(29)
    @DisplayName("UI-29 非作者不可见编辑/删除按钮")
    void ui29_nonOwnerCannotSeeOperatingButtons() {
        // 用户 A 发表博客，并记下 blogId
        registerAndLogin("ownerA");
        publishBlog("他人博客按钮隐藏测试", "内容");
        String blogId = openFirstBlogDetail();

        // 换成用户 B 登录，直接访问 A 的博客
        String otherUser = registerAndLogin("otherB");
        driver.get(baseUrl + "/blog_detail.html?blogId=" + blogId);
        // 等详情渲染完成（作者行出现即说明数据已加载、权限判断已执行）
        new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".content .author")));
        screenshot("UI-29-非作者无编辑删除");

        assertFalse(driver.findElement(By.cssSelector(".content .operating")).isDisplayed(),
                "非作者（当前登录：" + otherUser + "）不应看到编辑/删除按钮");
    }
}
