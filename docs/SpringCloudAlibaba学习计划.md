# 基于 BlogSystem 学习 Spring Cloud Alibaba 五大组件

> 目标:在现有博客单体项目的基础上,用**最简单易懂**的方式学完 Spring Cloud Alibaba 的五大组件。
> 原则:**不破坏现有能跑的项目**,新学的东西放在独立的新工程里,复用现有代码。
> 前置知识:已会 Spring Boot、MyBatis-Plus、JWT(你现在的水平刚好够)。

---

## 0. 五大组件分别是什么(用博客业务打比方)

| 组件 | 一句话解释 | 类比 | 在博客项目里干什么 |
|---|---|---|---|
| **Nacos** | 注册中心 + 配置中心 | 通讯录 + 黑板报 | 所有服务启动时来"报到",互相通过名字找到对方;配置(如数据库连接)集中管理,改完热生效 |
| **Spring Cloud Gateway** | 统一入口网关 | 小区门卫/前台 | 所有请求先进网关,由它转发到具体服务,顺便做鉴权、限流 |
| **Sentinel** | 流量防护 | 景区闸机 | 限制某个接口每秒最多访问几次,服务出问题时自动熔断降级 |
| **RocketMQ** | 消息队列 | 邮局 | 一个服务把"事件"投递到邮局,另一个服务异步取走处理(比如发博客后异步通知) |
| **Seata** | 分布式事务 | 多人记账对账 | 一个业务跨多个服务改数据时,保证要么全成功、要么全回滚 |

> 小知识:严格来说 Gateway 是 Spring Cloud 官方组件,不是 Alibaba 的,但国内教程通常把
> 「Nacos + Gateway + Sentinel + RocketMQ + Seata」合称五大件一起学,这样学最顺。
> 配套还需要 **OpenFeign**(服务间远程调用,Spring Cloud 官方组件)和 **LoadBalancer**(负载均衡),它们不是"五件套"但必用。

---

## 1. 总体思路(先想清楚再动手)

**不要**在原工程里直接拆。现有项目能跑、有完整测试和文档,拆坏了得不偿失。

**推荐做法**:在仓库里新建一个独立目录 `blog-cloud/`,作为全新的 Maven 多模块工程,
把现有代码**复制**过去再改造成微服务。原工程(monolith)留作"对照组"。

```
BlogSystem/
├── (现有单体工程,保持不动,main 分支)
└── blog-cloud/            ← 新学的都在这,推荐放独立 git 分支 feature/spring-cloud
    ├── pom.xml            父工程(管版本 BOM)
    ├── blog-common        公共模块:Result / 异常 / JWT / 工具类(从现有代码搬)
    ├── blog-user-service  用户服务:注册/登录/用户信息   (端口 8081)
    ├── blog-blog-service  博客服务:博客 CRUD/管理     (端口 8082)
    └── blog-gateway       网关:路由/鉴权/限流 + 前端静态页 (端口 8080)
```

- 前端 HTML 页面直接放进网关的静态资源目录,网关既转发 API 又当 Web 服务器,最简单。
- **最新进展**:本计划已落地实现,代码在仓库 `blog-cloud/` 目录,实现细节见 `docs/blog-cloud-实现文档.md`。
- 数据库:新建一个库 `blog_cloud`,表结构沿用 `init.sql`,再为 Seata 加一张 `undo_log` 表。
- 数据表只有 users、blog_info 两张,天然对应"用户服务管 users、博客服务管 blog_info",拆起来毫无压力。

---

## 2. 版本选型(照着抄,别自己乱配)

你的工程是 **Spring Boot 3.3.5 / Java 17**,对应版本是:

| 依赖 | 版本 | 说明 |
|---|---|---|
| Spring Cloud Alibaba | **2023.0.3.2** | 父 pom 引入 BOM,所有 Alibaba 组件版本它统一管 |
| Spring Cloud | 2023.0.3 | 父 pom 引入 BOM,管 Gateway/OpenFeign/LoadBalancer |
| Nacos Server | 2.4.3 | 下载后本地启动(standalone 模式) |
| Sentinel Dashboard | 1.8.8 | 可选,先不装也能学 |
| RocketMQ | 5.2.x | 本地解压启动(namesrv + broker) |
| Seata Server | 2.1.0 | 本地启动,file 模式即可 |

父 pom 核心就三行:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type><scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>2023.0.3.2</version>
            <type>pom</type><scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

每个模块再按需引入 `spring-cloud-starter-alibaba-nacos-discovery`、`spring-cloud-starter-gateway`、
`spring-cloud-starter-alibaba-sentinel`、`spring-cloud-starter-stream-rocketmq`、`spring-cloud-starter-alibaba-seata` 等,
**不要**手写组件版本号(BOM 已管)。

---

## 3. 分阶段学习计划(每阶段:学什么 → 改什么 → 怎么验收)

总节奏:业余时间每周 2~3 个阶段,约 2~4 周学完五件套。

### 阶段 0:环境准备(半天 ~ 1 天)

- 下载并启动 **Nacos 2.4.3**:
  ```bash
  cd nacos/bin
  startup.cmd -m standalone    # Windows;必须加 -m standalone,默认集群模式起不来
  ```
  浏览器访问 `http://localhost:8848/nacos`,账号密码都是 `nacos`。
- (可选)下载 **Sentinel Dashboard 1.8.8**:`java -jar sentinel-dashboard-1.8.8.jar`,访问 8080。
- 下载 **RocketMQ 5.2.x** 解压,先起 namesrv 再起 broker(学习机内存小要先调小脚本里的 -Xms/-Xmx)。
- 下载 **Seata 2.1.0** 解压,file 模式默认即可,先不启动。
- **验收**:Nacos 控制台能打开。四个中间件名字能对上号,知道各自是干什么的。

### 阶段 1:拆工程(1 ~ 2 天)—— 最有价值的一步

- 建 `blog-cloud` 多模块工程,把现有代码复制进去拆成 common / user-service / blog-service。
- user-service:UserController、UserService、UserInfoMapper + users 表。
- blog-service:BlogController、BlogService、BlogInfoMapper + blog_info 表。
- 数据库改成 `blog_cloud` 库,两个服务各自连同一套表(先各连各的表,别互相访问)。
- **验收**:`mvn spring-boot:run` 分别启动 8081、8082,单机能各自跑通注册/登录/博客 CRUD。
  (这一步先不接 Nacos,验证"拆开还能跑"。)

### 阶段 2:Nacos 注册中心 + 配置中心(1 ~ 2 天)

- 两个服务都引入 `nacos-discovery`,配 `spring.application.name`(如 `user-service`、`blog-service`)
  和 `spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848`,启动后能在 Nacos 控制台看到两个服务实例。
- 给 user-service 引入 `nacos-config`,把 `spring.datasource` 挪到 Nacos 配置中心的 dataId
  `user-service.yaml` 里,启动参数加 `--spring.cloud.nacos.config.server-addr=127.0.0.1:8848`。
- 写一个 `@RefreshScope` 的配置类(比如 `blog.switch: on`),在 Nacos 控制台改值,接口立刻读到新值。
- **验收**:控制台能看到 2 个服务注册;改 Nacos 里的配置,服务不用重启就生效。
- **概念收获**:注册中心解决"服务地址怎么互相找到";配置中心解决"配置集中管理 + 热更新"。

### 阶段 3:OpenFeign 服务调用 + LoadBalancer(半天 ~ 1 天)

- 博客详情页本来要显示作者名,现在作者在 user-service,所以 blog-service 通过 **Feign** 调
  `user-service` 的 `/user/getUserInfo` 拿到作者信息——这是最自然的练习场景。
- 引入 `spring-cloud-starter-openfeign` + `spring-cloud-starter-loadbalancer`,写一个
  `UserClient` 接口,`@FeignClient(name = "user-service")`。
- 在 blog-service 启动类加 `@EnableFeignClients`。
- **验收**:博客详情能通过"服务名调用"拿到作者信息;把 user-service 停掉再启动,Feign 自动换地址,不用改代码。
- **概念收获**:服务间调用靠"名字"而不是 IP 端口,负载均衡由 LoadBalancer 完成。

### 阶段 4:Gateway 网关(1 天)

- 新建 `blog-gateway` 模块,引入 `spring-cloud-starter-gateway` + `nacos-discovery`。
- 配置路由:`/user/**` → user-service,`/blog/**` → blog-service;前端静态页放网关 resources/static。
- 写一个全局过滤器做两件事:**(a)** 校验 JWT token(逻辑照搬现有 `LoginInterceptor`);
  **(b)** 校验通过后把 userId 放进请求头传给下游服务。
- 下游服务的 `LoginInterceptor` 可以删掉,信任网关转发的 header(这是网关存在的意义之一)。
- **验收**:浏览器访问 `http://localhost:8080/blog_list.html`,登录、发博客全流程走网关能通;
  不带 token 访问受保护接口返回 401。
- **概念收获**:统一入口、统一鉴权、统一跨域,业务服务只关心业务。

### 阶段 5:Sentinel 限流熔断(1 天)

- blog-service 引入 `spring-cloud-starter-alibaba-sentinel`,网关也引入 `sentinel-spring-cloud-gateway-adapter`(或直接走服务端限流,二选一,先简单)。
- 最简玩法:给 `/blog/getList` 配一个 **QPS 限流规则**(每秒 1 次),快速刷新页面,能看到被限流的提示(默认 429/Blocked)。
- 再玩一个**熔断降级**:给 Feign 调用 user-service 配降级规则,user-service 故意停掉时,
  blog-service 返回兜底数据(比如"作者:未知")而不是报 500。
- 规则可以先在代码里写死(`FlowRuleManager.loadRules(...)`),跑通了再上 Dashboard 动态配。
- **验收**:限流生效(刷新被拦)、降级生效(服务挂了有兜底)。配合网关过滤器看请求日志。
- **概念收获**:限流防"挤爆",熔断防"雪崩"。

### 阶段 6:RocketMQ 异步消息(1 ~ 2 天)

- 场景:**发博客成功后发一条消息**,user-service 消费后把该用户的 `blog_count` 字段 +1。
- blog-service 引入 `spring-cloud-starter-stream-rocketmq`,定义输出通道;
  user-service 引入同一 starter,定义输入通道(消费者),再加一个 `blog_count` 字段(users 表加列)。
- 启动 RocketMQ(namesrv + broker),在 `application.yml` 里配好 name-server 地址。
- **验收**:发一篇博客,user-service 的消费日志打印消息,`blog_count` 自动 +1;先停掉消费者,博客照常发布成功(消息积压,消费者启动后补消费)——体会"解耦 + 削峰"。
- **概念收获**:同步调用改成发消息,两边互不阻塞;消息不丢失地"等别人来取"。

### 阶段 7:Seata 分布式事务(1 ~ 2 天,压轴)

- 场景(经典):**发布博客 = blog-service 写 blog_info + 远程调 user-service 更新 blog_count**,
  用 `@GlobalTransactional` 包住整个调用链,模拟"写博客成功但更新计数失败",验证两库一起回滚。
- 准备:启动 Seata Server(file 模式);两个服务的库各建一张 `undo_log` 表;引入
  `spring-cloud-starter-alibaba-seata` 并配置 seata 服务地址、事务组名。
- `@GlobalTransactional` 加在 blog-service 的入口方法上(调用链最外层)。
- **验收**:故意让 user-service 抛异常,博客也不落库(整体回滚);控制台能看到分支事务提交/回滚日志。
- **概念收获**:跨服务的"全有或全无",理解 XID 如何跨服务传递。

---

## 4. 阶段完成后的学习闭环建议

- 每个阶段做完,写一小段笔记(为什么这么配、踩了什么坑),比抄代码重要。
- 用 Postman 或浏览器把**主流程**走一遍:注册 → 登录 → 发博客 → 查详情 → 管理上下架,
  记录它经过网关 → 服务 → (可选)消息/事务的完整链路。
- 全学完后,回头看你的单体工程,你应该能说出"哪块拆出去最合理、为什么"。

---

## 5. 常见坑(提前打预防针)

1. **版本别乱配**:统一用 BOM(2023.0.3.2),不要手写 Nacos/Sentinel 的版本号。
2. **Nacos 必须 standalone 启动**,否则报"集群模式需要配置"起不来。
3. **服务名别重复**:每个服务 `spring.application.name` 全局唯一且小写。
4. **Nacos 2.x 有 gRPC 端口(9848/9849)**,防火墙或端口占用会导致注册/发现诡异失败。
5. **Feign 别忘了 `spring-cloud-starter-loadbalancer`**,否则报找不到实例。
6. **Seata 的 `undo_log` 表必须建**,且 `@GlobalTransactional` 放调用链最外层入口。
7. **RocketMQ 先 namesrv 后 broker**,且学习机内存小要把 broker 脚本的 -Xms/-Xmx 调小。
8. **JDK23 编译 Lombok 的问题**:新工程沿用现有 pom 里的 `annotationProcessorPaths` 配置即可。
9. **拆分后旧测试会失效**:旧工程留在 main 分支不动,新工程单独建分支 `feature/spring-cloud`。
10. 中间件服务(尤其 Nacos/RocketMQ)建议用 JDK 17 启动,别用 23,少些莫名告警。

---

## 6. 学完五大件之后:引入 Redis(衔接方案)

学完 Spring Cloud 再上 Redis,顺序刚好,而且每个用途都能"接得上":

1. **缓存博客列表/详情**(最常用):`RedisTemplate` 手动缓存,或 Spring Cache `@Cacheable`,
   设 TTL 10 分钟,先缓存后查库。体会"缓存穿透/击穿/雪崩"概念。
2. **登录态进 Redis**:现在 token 是纯 JWT 无状态,可以把 token 存 Redis 实现"主动下线/踢人"。
3. **分布式锁**:用 Redisson 实现"防止重复提交/秒杀扣减",这是 Seata 之外的轻量并发方案。
4. 进阶:用 Redis 做 RocketMQ 之外的简单消息/延时任务(不推荐生产,但很适合练手)。

Redis 引进来之后,你的学习路线就完整覆盖了:**服务治理(Nacos)→ 通信(Gateway/Feign)→
容错(Sentinel)→ 异步(RocketMQ)→ 一致性(Seata)→ 性能(Redis)**。

---

## 7. 参考资料

- Spring Cloud Alibaba 官方版本说明(版本矩阵以这里为准):<https://sca.aliyun.com/docs/2023/overview/version-explain/>
- Spring Cloud Alibaba GitHub:<https://github.com/alibaba/spring-cloud-alibaba>
- Nacos:<https://nacos.io/>  Sentinel:<https://sentinelguard.io/zh-cn/>
- Seata:<https://seata.io/zh-cn/>  RocketMQ:<https://rocketmq.apache.org/>
- Spring Cloud 官方:<https://spring.io/projects/spring-cloud>
