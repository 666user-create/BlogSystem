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

```bash
mvn test
```

（后续将补充：测试用例设计文档、Postman 接口测试集合、测试报告等。）
