# blog-cloud 实现文档 —— 在博客项目上落地 Spring Cloud Alibaba 五大组件

> 对应代码:仓库根目录 `blog-cloud/`(独立多模块工程,不影响原单体项目 `src/`)。
> 前置阅读:`docs/SpringCloudAlibaba学习计划.md`。
> 本文档说明:每个组件是怎么接进来的、代码在哪、怎么启动、怎么验证。

---

## 1. 总体架构

```
                         浏览器(前端静态页由网关直接托管)
                                  │
                                  ▼
                    ┌──────────────────────────┐
                    │   blog-gateway  (8080)   │  组件三: Gateway 网关
                    │  路由 / 网关鉴权 / 限流    │  组件二: Sentinel 网关限流
                    └──────┬──────────┬────────┘
                           │          │
                    /user/**│          │/blog/**
                           ▼          ▼
              ┌─────────────────┐  ┌─────────────────┐
              │ blog-user-service│  │ blog-blog-service│
              │  (8081) 用户服务  │  │  (8082) 博客服务  │
              └───┬────┬────┬───┘  └───┬────┬────┬───┘
                  │    │    │          │    │    │
        ┌─────────┘    │    └────┐     │    │    └────────┐
        │  Nacos(8848) │         │     │    │             │
        │  注册中心+配置中心      │     │    │             │
        │              │  Feign  │     │ Feign           │
        ▼              ▼  调用    ▼     ▼   调用           ▼
   Nacos Server   RocketMQ(9876)   Sentinel(本地规则)  Seata Server(8091)
   组件一          组件四            组件二              组件五
```

- 服务间调用走 **Feign**(服务名 + Nacos 服务发现 + LoadBalancer 负载均衡),不经过网关。
- 中间件都是**可选点亮**:对应组件没启动时,服务照常能跑基本流程,启动对应中间件后该组件功能生效。

---

## 2. 模块结构

```
blog-cloud/
├── pom.xml                        # 父工程: 统一 BOM 版本管理
├── maven-settings.xml             # 本机 Clash 代理(构建用, 可选)
├── init.sql                       # 建库脚本: blog_cloud + undo_log(Seata用)
├── blog-common/                   # 纯公共层(不含 Web MVC, 网关也能引用)
│   └── src/main/java/com/example/blogcloud/common/
│       ├── constant/Constants.java            # token/请求头常量
│       ├── exception/BlogException.java       # 业务异常
│       ├── enums/ResultCodeEnum.java
│       ├── config/JwtConfig.java              # 启动时注入 JWT 密钥
│       ├── utils/  JwtUtils / SecurityUtil / MyBeanUtils / DateUtils
│       ├── pojo/request/  登录/注册/新增博客 请求体
│       ├── pojo/response/ Result / 用户 / 博客 响应体
│       ├── pojo/dataObject/ UserInfo / BlogInfo 实体
│       └── pojo/message/BlogPublishMessage.java  # RocketMQ 消息体
├── blog-web-common/               # Web 公共层(依赖 Spring MVC)
│   └── .../common/advice/
│       ├── ResponseAdvice.java    # 统一响应包装(内部接口不套壳)
│       └── ExceptionAdvice.java   # 全局异常处理
├── blog-user-service/             # 用户服务(8081)
│   └── .../user/
│       ├── UserServiceApplication.java   # 启动类 + 自动建 admin 账号
│       ├── controller/UserController.java
│       ├── service/ + impl/UserServiceImpl.java
│       ├── mapper/UserInfoMapper.java
│       ├── client/BlogClient.java         # Feign -> 博客服务
│       └── config/
│           ├── AppConfig.java             # Nacos 配置中心热更新演示
│           └── RocketMqConsumerConfig.java # RocketMQ 消费者(mq profile)
├── blog-blog-service/             # 博客服务(8082)
│   └── .../blog/
│       ├── BlogServiceApplication.java
│       ├── controller/BlogController.java
│       ├── service/impl/BlogServiceImpl.java   # @GlobalTransactional 全局事务
│       ├── service/AuthorInfoService.java      # @SentinelResource 熔断演示
│       ├── client/UserClient.java              # Feign -> 用户服务
│       ├── config/SentinelConfig.java          # 限流/熔断规则
│       └── mapper/BlogInfoMapper.java
└── blog-gateway/                  # 网关(8080)
    ├── .../gateway/
    │   ├── GatewayApplication.java
    │   ├── filter/AuthGlobalFilter.java       # 网关鉴权, 注入用户身份请求头
    │   └── config/
    │       ├── GatewayJwtConfig.java
    │       └── GatewaySentinelConfig.java     # 网关限流
    └── src/main/resources/static/             # 复制的原前端页面
```

---

## 3. 版本选型(已实测可编译)

| 依赖 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.3.5 | 与单体工程一致 |
| Java | 17 | 本机用 JDK 21 编译, target 17 |
| Spring Cloud | 2023.0.3 | BOM: `org.springframework.cloud:spring-cloud-dependencies` |
| Spring Cloud Alibaba | 2023.0.3.2 | BOM: `com.alibaba.cloud:spring-cloud-alibaba-dependencies` |
| Nacos Server | 2.4.3 | 注册中心 + 配置中心 |
| Sentinel | 1.8.8 | 随 SCA BOM; 网关适配器需显式版本 |
| RocketMQ | 5.2.x | 通过 Spring Cloud Stream 接入 |
| Seata Server | 2.1.0 | file 模式即可 |

> 版本坑:SCA 的 BOM 坐标是 `com.alibaba.cloud`, 网上很多教程写 `org.springframework.cloud`
> (那是 2020 年前的旧坐标, 只有 0.x 版本)。

---

## 4. 五大组件逐个说明(实现要点)

### 组件一:Nacos —— 注册中心 + 配置中心

**注册中心**(三个服务都接了):
- 依赖:`spring-cloud-starter-alibaba-nacos-discovery`
- 配置:`spring.cloud.nacos.discovery.server-addr: 127.0.0.1:8848`
- 效果:启动后在 Nacos 控制台"服务管理"能看到 `user-service` / `blog-service` / `blog-gateway` 三个服务。
- 意义:Feign 用服务名 `user-service` 就能找到实例,IP 变了不用改代码。

**配置中心**(user-service 演示):
- 依赖:`spring-cloud-starter-alibaba-nacos-config`
- 配置:`spring.config.import: optional:nacos:user-service.yaml`(`optional:` 表示 Nacos 挂了也不影响启动)
- 演示对象:`AppConfig`(@ConfigurationProperties 前缀 `app`),`/user/configInfo` 接口返回 `app.switch-on` 值。
- 验证:在 Nacos 控制台新建配置 `user-service.yaml`(group 默认),内容 `app.switch-on: closed`,
  服务无需重启,`/user/configInfo` 返回值自动变成 `off`(Spring Cloud 刷新事件 + 自动重绑 Bean)。

### 组件二:Sentinel —— 限流 + 熔断降级

**服务端限流/熔断**(blog-service):
- 依赖:`spring-cloud-starter-alibaba-sentinel`(本地模式, 不需要服务端)
- `SentinelConfig` 启动时加载两条规则(代码方式, 最直观):
  - 限流:`GET:/blog/getList` QPS=1 → 快速刷新列表接口会被拦(HTTP 429 "Blocked by Sentinel")
  - 熔断:`getAuthorName` 资源 1 秒内异常数 ≥3 → 熔断 5 秒
- `AuthorInfoService.getAuthorName()` 标了 `@SentinelResource(fallback=...)`:
  用户服务挂掉时,Feign 调用抛异常被 Sentinel 统计,触发熔断后直接走 fallback 返回"未知作者",博客详情不报 500。

**网关限流**(gateway):
- 依赖:`com.alibaba.csp:sentinel-spring-cloud-gateway-adapter`(1.8.8, 不在 SCA BOM, 父 pom 显式管理)
- `GatewaySentinelConfig`:`GatewayFlowRule("blog_service")` QPS=2(路由 id 对应配置里的 blog_service),
  被限流返回自定义 JSON `{"code":-1,"errMsg":"请求过于频繁..."}`。

### 组件三:Spring Cloud Gateway —— 网关

- 依赖:`spring-cloud-starter-gateway`(WebFlux, 不能和 spring-boot-starter-web 共存, 所以网关不引 blog-web-common)
- 路由(application.yml):`/user/**` → `lb://user-service`,`/blog/**` → `lb://blog-service`
- `AuthGlobalFilter`(全局过滤器, 核心演示点):
  1. 白名单(登录/注册/静态资源)直接放行;
  2. 其余业务接口校验 JWT, 无效返回 401 JSON;
  3. 校验通过后把 `X-User-Id` / `X-User-Name` 请求头转发给下游。
- 下游服务(BlogController)不再解析 token, 直接信任网关转发的请求头(缺失时回退解析 token 方便直连调试)。
- 前端静态页(6 个 html + css/js/pic/blog-editormd 等 537 个文件)直接放在网关 classpath,由 WebFlux 静态资源处理器托管。

### 组件四:RocketMQ —— 异步消息(Spring Cloud Stream)

- 依赖:`spring-cloud-starter-stream-rocketmq`(SCA 管理的 RocketMQ binder)
- 场景:**删除博客**时,blog-service 向 topic `blog-topic` 发消息,user-service 消费后把作者 `blog_count` - 1。
- 生产端(blog-service, `application-mq.yml` 激活):
  - `spring.cloud.stream.bindings.blog-out-0.destination: blog-topic`
  - `BlogServiceImpl.deleteBlog()` 里 `streamBridge.send("blog-out-0", new BlogPublishMessage(...))`
- 消费端(user-service, 同样激活 mq profile):
  - 函数式消费者 `@Bean Consumer<BlogPublishMessage> blogIn()` 对应 binding `blog-in-0`
  - 更新 `user_info.blog_count`(SQL 里用 `IF(blog_count>0, blog_count-1, 0)` 防负)
- **关键设计**:消费者 Bean 用 `@Profile("mq")` 包裹 —— 不激活 mq profile 时没有 Consumer Bean,
  Spring Cloud Stream 就不会创建消费连接,不启动 RocketMQ 服务也能正常起。
  启动时加 `--spring.profiles.active=mq` 才启用消息收发。
- 验证:先启动 RocketMQ,再用 mq profile 启动两个服务;删除一篇博客,user-service 日志打印消费,
  `blog_count` 自动 -1;先停消费者再删博客,消息在 broker 积压,消费者启动后补消费(体验"解耦 + 削峰")。

### 组件五:Seata —— 分布式事务

- 依赖:`spring-cloud-starter-alibaba-seata`(随 BOM 管理 Seata 2.1.0 客户端)
- 场景:**发博客** = 博客服务写 `blog_info` + Feign 远程调用户服务把作者 `blog_count` + 1,
  用 `@GlobalTransactional` 包住整个调用链,保证两个库要么都成功要么都回滚。
- 配置(两个服务一致, 事务组 `blog_tx_group`):
  ```yaml
  seata:
    enabled: true
    application-id: blog-service        # 或 user-service
    tx-service-group: blog_tx_group
    registry: { type: file }
    service:
      vgroup-mapping: { blog_tx_group: default }
      grouplist: { default: 127.0.0.1:8091 }
  ```
- 数据库:每个业务库必须建 `undo_log` 表(init.sql 已含)。
- **关键设计**:`seata.enabled` 默认 `false` —— 不启动 Seata 也能跑基本流程;
  演示时:启动 Seata Server → 两个服务把 `seata.enabled` 改为 `true` 重启 → 正常发博客(两处同时 +1);
  把 user-service 停掉再发博客 → 整个事务回滚,博客不落库。
- 分支接口:`user-service` 的 `/user/internal/increaseBlogCount`(路径含 `/internal/` 不套 Result 壳,
  见下面"设计决策")。

---

## 5. 数据库

执行 `blog-cloud/init.sql`(建库 `blog_cloud`):
- `user_info`:新增 `blog_count` 字段(微服务版冗余计数, 供 Seata/RocketMQ 演示)
- `blog_info`:与单体一致
- `undo_log`:Seata 必需

```bash
mysql -uroot -proot < blog-cloud/init.sql
```

---

## 6. 启动步骤

### 6.1 启动中间件(按需)

```bash
# Nacos(组件一, 必须 standalone)
cd nacos/bin && startup.cmd -m standalone          # http://localhost:8848/nacos  nacos/nacos

# RocketMQ(组件四: namesrv -> broker)
cd rocketmq/bin
start mqnamesrv.cmd
start mqbroker.cmd -n 127.0.0.1:9876

# Seata(组件五)
cd seata/bin && seata-server.bat

# Sentinel Dashboard(可选, 本工程规则在代码里, 不装也能玩)
java -jar sentinel-dashboard-1.8.8.jar
```

### 6.2 构建

```bash
cd blog-cloud
mvn -s maven-settings.xml -DskipTests clean package
# (本机直连 Maven Central 的 HTTPS 不通, 走 Clash 代理; 无代理环境去掉 -s 即可)
```

### 6.3 启动三个服务(按依赖顺序)

> 启动顺序很重要:写操作(发博客)依赖**两个服务互相可见**,所以先起 Nacos 再起服务,
> 否则发博客会报 `user-service executing POST ...` 错误(博客已写入但远程计数失败, 这正是 Seata 要解决的缺口)。

```bash
# 用户服务
java -jar blog-user-service/target/blog-user-service-0.0.1-SNAPSHOT.jar
# 博客服务
java -jar blog-blog-service/target/blog-blog-service-0.0.1-SNAPSHOT.jar
# 网关(最后起, 需要 Nacos 里已有两个服务才能路由)
java -jar blog-gateway/target/blog-gateway-0.0.1-SNAPSHOT.jar
```

打开浏览器访问 `http://localhost:8080/blog_list.html`,注册/登录/发博客,全部走网关。

### 6.4 开启可选组件

```bash
# RocketMQ 演示: 两个服务都加 mq profile 重启
java -jar blog-user-service-...jar --spring.profiles.active=mq
java -jar blog-blog-service-...jar --spring.profiles.active=mq

# Seata 演示: 两个服务 application.yml 里 seata.enabled 改为 true 后重启
```

---

## 7. 验证清单(每个组件怎么确认"真的生效了")

| 组件 | 验证方法 | 预期结果 |
|---|---|---|
| Nacos 注册中心 | Nacos 控制台 → 服务管理 | 能看到 user-service / blog-service / blog-gateway |
| Nacos 配置中心 | Nacos 新建 `user-service.yaml` 写 `app.switch-on: closed`; 浏览器访问 `http://localhost:8080/user/configInfo`(网关白名单已放行) | 不重启, 返回值从 `open` 变 `closed` |
| Gateway 路由 | 登录接口不带 token 访问 `/blog/getList` | 返回 401 JSON `未登录或登录已失效` |
| Gateway 鉴权 | 带 token 访问 `/blog/getList` | 正常返回列表 |
| Sentinel 服务端限流 | 连点几次 `/blog/getList`(QPS>1) | 出现 `Blocked by Sentinel (flow limiting)` |
| Sentinel 熔断 | 停掉 user-service, 刷新博客详情页几次 | 前几次报错后, 详情返回作者名"未知作者"而不是 500 |
| Sentinel 网关限流 | 快速刷新列表(>2 次/秒) | 返回 `请求过于频繁, 已被 Sentinel 网关限流` |
| RocketMQ | 删一篇博客(需 mq profile + RocketMQ 已启动) | user-service 日志打印消费, 作者 blog_count -1 |
| Seata | 启动 Seata + enabled=true; 正常发博客 | blog_info 多一条 且 作者 blog_count +1; 停 user-service 再发 → 博客不落库(整体回滚) |
| Feign | 博客详情接口返回值里 `authorName` 字段 | 等于作者用户名(跨服务拿到) |

---

## 8. 设计决策(为什么这么写)

1. **为什么新开 `blog-cloud/` 而不是改原工程**:原单体有完整测试和文档,直接拆风险大;复制改造,原工程留作对照。
2. **为什么 split 成 `blog-common` 和 `blog-web-common` 两层**:
   `blog-common` 不含任何 Spring MVC 类,网关(WebFlux)可以安全引用 JwtUtils/Constants/POJO;
   如果混在一起,网关会因 classpath 上有 spring-webmvc 而被迫以 Servlet 模式启动,直接起不来。
3. **为什么网关鉴权、服务端不再校验**:网关是唯一入口,统一校验一次即可;
   校验通过后注入 `X-User-Id` 请求头,下游服务信任该头(直连调试时保留 token 解析作为回退)。
4. **为什么内部接口(`/internal/`)不套 Result 壳**:`ResponseAdvice` 统一包装会让 Feign 收到
   `{"code":200,"data":{...}}`,反序列化回业务对象会丢字段。规则:路径含 `/internal/` 直接返回原始对象。
5. **为什么 Seata 默认关闭、RocketMQ 用 profile 开关**:学习项目要"先跑通再点亮"。
   中间件没起时服务也能跑;每启动一个中间件、点亮一个组件,概念更清楚。
6. **为什么 @GlobalTransactional 放在 blog-service 的 addBlog**:全局事务要放在调用链最外层入口,
   由它开启 XID 并透传给 Feign 调用的另一个服务。
7. **为什么 Sentinel 规则写在代码里**:比装 Dashboard、配推送链路简单得多,适合入门;
   生产环境再换成控制台动态推送。

---

## 9. 常见问题

| 问题 | 原因 / 解决 |
|---|---|
| 服务启动报 Nacos 注册失败并**直接退出** | 实测: SCA 2023.0.3.2 的注册失败默认 fail-fast=true 会中断启动; 三个服务的 yml 已显式设 `spring.cloud.nacos.discovery.fail-fast: false`, 未起 Nacos 时仅告警不退出(起好 Nacos 后重启服务即可注册) |
| 网关访问 `/blog/getList` 返回 503 | 下游服务没注册到 Nacos, 或没启动; 先确认控制台能看到两个服务 |
| 启动报 gRPC 端口占用 / 连接失败 | Nacos 2.x 除了 8848 还要 9848/9849(gRPC), 防火墙/占用要处理 |
| mvn 下载依赖失败 | 本机 HTTPS 直连中央仓库不通, 用 `-s maven-settings.xml` 走代理 |
| `mvn` 不是内部命令 | 用 IDEA 自带 Maven 或 Maven wrapper; 本机 wrapper 缓存位于 `~/.m2/wrapper/dists` |
| 发博客报 `user-service executing POST ...` | 用户服务不可达(未起 Nacos / 用户服务未启动)。发博客是跨服务写操作, 依赖用户服务可见; 起好 Nacos 后重试。Seata 开启后该调用会与博客写入一起回滚 |
| 发博客后 `blog_count` 没变 | Seata 未开启(默认)时计数不维护; 或开启后 Seata Server 没启动导致事务失败回滚 |
| 删博客后 `blog_count` 没减 | 需要 mq profile + RocketMQ 已启动 |
| 改了 Nacos 配置不生效 | 确认 dataId 是 `user-service.yaml`、group 是 DEFAULT_GROUP、配置里有 `app.switch-on` |
| 网关 401 | token 过期/缺失; 先 `/user/login` 拿新 token, 请求头带 `user_token` |

---

## 10. 已实现 vs 后续可做

已实现(本仓库代码):工程拆分、Nacos 注册/配置、Gateway 路由/鉴权、Sentinel 限流/熔断(服务端+网关)、
RocketMQ 异步消息(删博客事件)、Seata 全局事务(发博客计数)、前端页面迁移。

后续可做(不在本次范围):
- Sentinel 控制台动态规则推送
- Seata 换 DB 模式(TC 状态入库)与 TCC 模式
- Nacos 配置加密 / 多环境 namespace
- 服务熔断降级到网关层统一兜底、链路追踪(Sleuth/Micrometer Tracing)
- Redis(缓存列表/详情、token 主动下线、分布式锁),见学习计划第 6 节
