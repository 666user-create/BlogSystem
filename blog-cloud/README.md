# blog-cloud —— Spring Cloud Alibaba 五组件学习工程

基于 `BlogSystem` 单体博客拆出来的微服务学习项目。

- 模块:`blog-common`(公共层)、`blog-web-common`(Web 公共层)、`blog-user-service`(8081)、`blog-blog-service`(8082)、`blog-gateway`(8080)
- 组件:Nacos(注册/配置)、Gateway、Sentinel(限流熔断)、RocketMQ(异步消息)、Seata(分布式事务)
- 版本:Spring Boot 3.3.5 / Spring Cloud 2023.0.3 / Spring Cloud Alibaba 2023.0.3.2 / Java 17

## 快速开始

```bash
# 1. 建库(MySQL 需已启动)
mysql -uroot -proot < init.sql

# 2. 构建(本机 HTTPS 直连中央仓库不通时加 -s maven-settings.xml 走 Clash 代理)
mvn -s maven-settings.xml -DskipTests clean package

# 3. 启动中间件(按需): Nacos(必, standalone) / RocketMQ / Seata
#    Nacos:  nacos/bin/startup.cmd -m standalone

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

完整说明(组件实现细节 / 验证清单 / 常见问题):见 `docs/blog-cloud-实现文档.md`(仓库根目录)。
