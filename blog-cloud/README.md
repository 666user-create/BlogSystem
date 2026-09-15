# blog-cloud —— Spring Cloud Alibaba 五组件学习工程

基于 `BlogSystem` 单体博客拆出来的微服务学习项目。

- 模块:`blog-common`(公共层)、`blog-web-common`(Web 公共层)、`blog-user-service`(8081)、`blog-blog-service`(8082)、`blog-gateway`(8080)
- 组件:Nacos(注册/配置)、Gateway、Sentinel(限流熔断)、RocketMQ(异步消息)、Seata(分布式事务)
- 版本:Spring Boot 3.3.5 / Spring Cloud 2023.0.3 / Spring Cloud Alibaba 2023.0.3.2 / Java 17

## 一键启动(推荐,Windows)

不用记命令,**双击即可**:

| 脚本 | 作用 |
|---|---|
| `start-all.bat` | 一键启动:检查 jar → 启动 Nacos(若未运行并等就绪)→ 按顺序启动用户服务/博客服务/网关(各一个日志窗口)→ 打印状态表 → 自动打开浏览器 |
| `stop-all.bat` | 一键停止三个 Java 服务(保留 Nacos) |
| `stop-all.bat all` | 三个服务 + Nacos 全部停止 |

命令行里也可以:`powershell -NoProfile -ExecutionPolicy Bypass -File start-all.ps1`

## 手动启动

```bash
# 1. 建库(MySQL 需已启动)
mysql -uroot -proot < init.sql

# 2. 构建(依赖已缓存时可加 -o 离线构建;需联网时用 -s maven-settings.xml 走代理)
mvn -o -DskipTests clean package

# 3. 启动中间件(按需): Nacos(必, standalone) / RocketMQ / Seata
#    Nacos:  D:\nacos-server-2.4.3\nacos\bin\startup.cmd -m standalone

# 4. 启动服务
java -jar blog-user-service/target/blog-user-service-0.0.1-SNAPSHOT.jar
java -jar blog-blog-service/target/blog-blog-service-0.0.1-SNAPSHOT.jar
java -jar blog-gateway/target/blog-gateway-0.0.1-SNAPSHOT.jar

# 5. 浏览器访问 http://localhost:8080/blog_list.html
```

## 测试

```bash
mvn test
```

按单体工程同一套套路（JUnit5 + Mockito）补充的单元/服务层测试，共 48 条，不依赖任何中间件（Nacos/RocketMQ/Seata 均不需要启动）：

| 模块 | 测试类 | 覆盖 |
|---|---|---|
| blog-common | JwtUtilsTest / SecurityUtilTest | JWT 签发解析/篡改/过期、密码加盐哈希 |
| blog-user-service | UserServiceImplTest | 登录/注册/用户信息/Feign 查作者/Seata 分支博客数+1 |
| blog-blog-service | BlogServiceImplTest / AuthorInfoServiceTest | CRUD/作者权限/逻辑删除/RocketMQ 分支/Sentinel 兜底 |
| blog-gateway | AuthGlobalFilterTest | 网关白名单放行/无 token 401/有效 token 注入请求头 |

> 接口级测试（RestAssured + @SpringBootTest）需要 Nacos/MySQL 等环境，后续可基于 Testcontainers 补充。

完整教学文档(零基础入门:概念 / 代码 / 启动 / 验收 / 常见问题):见本工程内的 `docs/SpringCloud入门教学.md`。
