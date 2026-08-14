# BlogSystem 博客系统

一个基于 Spring Boot 的个人博客系统，支持用户注册登录、博客的发布/编辑/删除（逻辑删除）、管理员上下架管理等核心功能，前端为原生 HTML + jQuery + editor.md（Markdown 编辑器）。

本项目同时作为个人**测试开发（测开）练习项目**，包含 JUnit5 单元测试、接口测试用例、测试文档等测试资产。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.3.5（Java 17） |
| 持久层 | MyBatis-Plus 3.5.5 + MySQL |
| 鉴权 | JWT（jjwt 0.11.5），登录拦截器校验 |
| 参数校验 | spring-boot-starter-validation（Hibernate Validator） |
| 密码安全 | 32 位随机盐 + MD5 |
| 前端 | 原生 HTML / CSS / JavaScript + jQuery + editor.md |
| 测试 | JUnit5 + spring-boot-starter-test |

## 功能清单

- 用户注册、登录（JWT 签发），根据用户/博客查询作者信息
- 博客列表（仅展示已上架、未删除）、博客详情
- 博客新增、编辑、删除（逻辑删除，仅作者本人可操作）
- 管理员：查看全部博客（含下架）、切换博客上下架
- 全局统一响应包装（`Result`）、全局异常处理

## 快速启动

前置要求：JDK 17+、Maven、MySQL 8。

1. 初始化数据库：

   ```sql
   -- 执行项目根目录的 init.sql
   source init.sql;
   ```

2. 修改数据库连接配置：`src/main/resources/application.yml` 中的 `spring.datasource`（默认 root/root）。

3. 启动项目：

   ```bash
   mvn spring-boot:run
   ```

4. 浏览器访问：http://localhost:8080/blog_list.html

> 应用首次启动会自动创建默认管理员账号：**admin / admin123**。

## 接口清单

统一响应结构：`{ "code": 200, "data": ..., "errMsg": null }`，错误时 `code = -1`。

登录相关接口需在请求头携带 token：`user_token`（或兼容旧字段 `userToken`）。

### 用户模块 `/user`

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| POST | /user/register | 用户注册 | 无需 |
| POST | /user/login | 用户登录，返回 token | 无需 |
| GET | /user/getUserInfo?userId= | 查询用户信息 | 需登录 |
| GET | /user/getAuthorInfo?blogId= | 根据博客查作者 | 需登录 |

### 博客模块 `/blog`

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | /blog/getList | 博客列表（已上架） | 需登录 |
| GET | /blog/getBlogDetail?blogId= | 博客详情 | 需登录 |
| POST | /blog/add | 新增博客（body: BlogInfoResponse） | 需登录 |
| POST | /blog/update | 更新博客（仅作者） | 需登录 |
| POST | /blog/delete?blogId= | 删除博客（仅作者，逻辑删除） | 需登录 |
| GET | /blog/adminList | 管理员全部列表 | 管理员 |
| POST | /blog/togglePublish?blogId= | 管理员切换上下架 | 管理员 |

## 目录结构

```
src/main/java/org/example/blogsystem
├── common          # 公共层：异常、拦截器、响应包装、工具类、POJO
├── controller      # 接口层：UserController / BlogController
├── mapper          # MyBatis-Plus Mapper
├── service         # 业务层
└── BlogSystemApplication.java

src/test/java       # JUnit5 单元测试
src/main/resources  # 配置文件 + 前端静态页面
```

## 测试

本项目按企业级测试流程产出完整测试文档（见 `docs/` 目录）：

| 文档 | 内容 |
|---|---|
| `docs/01-需求分析与测试范围说明书.md` | 需求基线、业务规则、接口清单 |
| `docs/02-测试计划.md` | 测试策略（JUnit5 + Postman + Selenium）、环境、进度、准入准出 |
| `docs/03-测试用例设计.md` | 90 条测试用例（等价类/边界值/判定表/场景法/错误推测） |
| `docs/04-测试执行记录.md` | 执行统计、回归记录 |
| `docs/05-测试总结报告.md` | 缺陷分析、质量评估、结论 |
| `docs/缺陷清单.md` | 11 条缺陷（BUG-01 ~ BUG-11），含严重级别与修复状态 |

自动化测试：

```bash
mvn test
```

| 层级 | 框架 | 覆盖 |
|---|---|---|
| 单元测试 | JUnit5 + Mockito | 工具类（JWT/密码）+ 服务层（`UserServiceImpl`、`BlogServiceImpl` 全分支） |
| 接口测试 | RestAssured + JUnit5 | 用户/博客/管理端/鉴权 30 条接口用例（`@SpringBootTest` 随机端口 + 独立 test profile） |
| UI 自动化 | Selenium WebDriver | 6 条核心流程用例（设计见测试文档，脚本待补） |

> 接口测试需要本机 MySQL 已启动，连接配置见 `src/test/resources/application-test.yml`（与开发/生产配置隔离）。
