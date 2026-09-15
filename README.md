# BlogSystem 博客系统

一个基于 Spring Boot 的个人博客系统，支持用户注册登录、博客的发布/编辑/删除（逻辑删除）、管理员上下架管理等核心功能，前端为原生 HTML + jQuery + editor.md（Markdown 编辑器）。

本项目同时作为个人**测试开发（测开）练习项目**，包含 Selenium UI 自动化、pytest 接口自动化、抓包分析、JMeter 性能测试与全套测试文档。

> **微服务学习分支**：`blog-cloud/` 是基于本工程拆出来的 **Spring Cloud Alibaba 五组件学习项目**
> （Nacos / Gateway / Sentinel / RocketMQ / Seata）。零基础入门教学见
> [docs/SpringCloud入门教学.md](docs/SpringCloud入门教学.md)（概念讲解 + 代码片段 + 启动步骤 + 验收清单）。原单体工程保持不动。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.3.5（Java 17） |
| 持久层 | MyBatis-Plus 3.5.5 + MySQL |
| 鉴权 | JWT（jjwt 0.11.5），登录拦截器校验 |
| 参数校验 | spring-boot-starter-validation（Hibernate Validator） |
| 密码安全 | 32 位随机盐 + MD5 |
| 前端 | 原生 HTML / CSS / JavaScript + jQuery + editor.md |
| 测试 | JUnit5、Selenium WebDriver 4（Headless Chrome）、pytest + requests + Allure、JMeter、Postman |

## 功能清单

- 用户注册、登录（JWT 签发），根据用户/博客查询作者信息
- 博客列表（仅展示已上架、未删除，**分页返回** `{list,total,pageNum,pageSize,pages}`）、博客详情
- 博客新增、编辑、删除（逻辑删除，仅作者本人可操作）
- 管理员：分页查看全部博客（含下架）、切换博客上下架
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

src/test/java       # Java 测试（JUnit5 单元测试 + Selenium UI 自动化，遵循 Maven 约定）
src/main/resources  # 配置文件 + 前端静态页面

tests/              # 测试资产统一入口（跨语言的都收在这里）
├── api-python/     #   接口自动化：pytest + requests + Allure（原 tests-python/）
├── perf/           #   性能测试：JMeter 计划 .jmx（原 perf/）
├── evidence/       #   [生成物] UI 截图、弹框文本、抓包 json —— 已忽略，不入库
└── reports/        #   [生成物] JMeter 结果/报告、Allure 结果/报告 —— 已忽略，不入库

docs/               # 测试文档（01~08 + 缺陷清单），只放"人读的文档"
scripts/            # 可复现脚本：清理测试数据、binlog 数据恢复
blog-cloud/         # Spring Cloud 微服务模块（独立工程 + 自己的 docs/）
```

## 测试

本项目按企业级测试流程产出完整测试文档（见 `docs/` 目录）：

| 文档 | 内容 |
|---|---|
| `docs/01-需求分析与测试范围说明书.md` | 需求基线、业务规则、接口清单 |
| `docs/02-测试计划.md` | 测试策略（Selenium + pytest + Postman + JMeter）、环境、进度、准入准出 |
| `docs/03-测试用例设计.md` | 93 条测试用例（等价类/边界值/判定表/场景法/错误推测） |
| `docs/04-测试执行记录.md` | 执行统计、回归记录、自动化执行明细 |
| `docs/05-测试总结报告.md` | 缺陷分析、质量评估、性能结论 |
| `docs/06-UI自动化测试说明.md` | Selenium 框架设计、33 条 UI 用例、截图证据 |
| `docs/07-接口测试与抓包分析说明.md` | pytest 接口自动化、抓包代理与明文分析 |
| `docs/08-性能测试报告.md` | JMeter 压测结果（吞吐量 90.1 请求/秒、平均 5.9ms） |
| `docs/09-简历项目描述.md` | 可直接使用的简历条目 + 面试追问准备 |
| `docs/缺陷清单.md` | 18 条缺陷（BUG-01 ~ BUG-18），含严重级别与修复状态 |

自动化测试（合计 78 条，全部通过）：

```bash
mvn test -Dtest=BlogUiTest              # Selenium UI 自动化 33 条
cd tests/api-python && python -m pytest     # 接口自动化 45 条（含抓包与安全检查）
```

| 层级 | 框架 | 覆盖 |
|---|---|---|
| UI 自动化 | Selenium + Java | 33 条核心流程（Headless Chrome + 显式等待 + 截图断言 + 样式断言），证据运行后本地生成 |
| 接口自动化 | Python + pytest + requests + Allure | 45 条（含分页、抓包明文分析、SQL 注入检查） |

> 仓库只保留**可复现的脚本与测试结论**，不提交"跑一次就有"的产物（截图 / 抓包 json / JMeter 原始结果），
> 它们已在 `.gitignore` 中忽略，跑一次对应命令即可在本地生成：

| 产物（本地生成，不入库） | 生成方式 |
|---|---|
| UI 截图 34 张 + 弹框文本证据 | `mvn test -Dtest=BlogUiTest` → `tests/evidence/ui/` |
| 抓包证据（登录请求明文分析） | `cd tests/api-python && python -m pytest test_security_capture.py` → `tests/evidence/capture/login-capture.json` |
| JMeter 原始结果与 HTML 报告 | `jmeter -n -t tests/perf/BlogSystem-性能测试计划.jmx -l tests/reports/jmeter/result.jtl -e -o tests/reports/jmeter`（压测计划本身入库） |
| Allure HTML 报告 | `cd tests/api-python && python -m pytest`，再 `allure generate ../reports/allure-results -o ../reports/allure-python --clean` |

> 需要本机 MySQL 已启动（连接配置见 `src/test/resources/application-test.yml`，与开发/生产配置隔离）。
