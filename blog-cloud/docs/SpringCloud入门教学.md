# Spring Cloud 零基础入门 —— 拿你的博客项目学会微服务

> **这份文档写给谁**:会 Spring Boot 基本用法(能写 Controller / Service / Mapper),但没碰过微服务的人。
> **怎么读**:第 1~2 节先建立整体印象,第 3 节跟着一条请求走一遍,第 4 节逐个组件看代码,第 7 节动手跑起来。
> **配套代码**:仓库根目录 `blog-cloud/`(基于 `BlogSystem` 单体博客拆出来的微服务学习工程)。
> **本文档位置**:`blog-cloud/docs/SpringCloud入门教学.md`——属于微服务工程内部文档,所以跟着 `blog-cloud/` 走,不再放在仓库根 `docs/`(那里只放单体工程的测试文档)。
> **所有代码片段都来自本工程真实文件**,可以对照着看。

---

## 目录

1. [五分钟搞懂:微服务到底难在哪](#1-五分钟搞懂微服务到底难在哪)
2. [问题 → 组件 对照表](#2-问题--组件-对照表)
3. [一条请求串起所有组件](#3-一条请求串起所有组件)
4. [七个组件逐个讲](#4-七个组件逐个讲)
5. [单体写法 → 微服务写法](#5-单体写法--微服务写法)
6. [术语小词典](#6-术语小词典)
7. [把项目跑起来](#7-把项目跑起来)
8. [验收清单:怎么确认组件真的生效了](#8-验收清单怎么确认组件真的生效了)
9. [常见问题急救表](#9-常见问题急救表)
10. [只记五句话](#10-只记五句话)
11. [下一步学什么 / 该看哪几个文件](#11-下一步学什么--该看哪几个文件)

---

## 1. 五分钟搞懂:微服务到底难在哪

### 先看原来(单体应用)

你原来的 `BlogSystem` 是**一个进程**:用户、博客的代码都在一个 Spring Boot 里。

```java
// 单体里,拿用户信息就是一句本地方法调用,快、简单、不会失败
UserInfoResponse user = userService.getUserInfo(1);
```

### 再看现在(微服务)

拆成两个独立进程后,同一件事变成了"跨越两个进程":

```
blog-user-service  (进程 A, 端口 8081)   ← 用户数据在这里
blog-blog-service  (进程 B, 端口 8082)   ← 博客数据在这里
```

于是冒出一堆单体时代不存在的问题:

| 拆开后的新问题 | 说人话 | 谁来管 |
|---|---|---|
| 进程 B 怎么知道进程 A 在哪?A 换机器/换端口了怎么办? | "我怎么找到你?" | **Nacos 注册中心** |
| 两个进程之间怎么调用?写死 `http://127.0.0.1:8081` 吗? | "怎么跟你说话?" | **OpenFeign** |
| 浏览器要访问这么多端口?登录校验在每个服务里各写一遍? | "总得有个大门吧" | **Gateway 网关** |
| 某个接口被疯狂请求 / 某个服务卡死了,会不会拖垮全部? | "挤爆了怎么办" | **Sentinel** |
| 一次业务要改两个进程的数据,改一半失败了怎么办? | "两边对不上账" | **Seata** |
| 有些事不需要立刻等结果(比如统计数字) | "能不能先寄出去" | **RocketMQ** |
| 改个配置要重启 3 个服务? | "配置别跟着包走" | **Nacos 配置中心** |

**Spring Cloud 就是把这些"分布式必修课"做成了现成的 starter + 注解 + 配置。** 你不需要自己写注册表、自己写 RPC、自己写负载均衡。

> **starter(起步依赖)是什么**:Spring Boot 的"一揽子依赖包"。引入一个 starter,相关的 jar 和自动配置全都给你装好,你只写配置就行。

### 这个工程拆成了哪几块(先混个眼熟)

```
BlogSystem/                     ← 仓库根目录
├── src/                        ← 原来的单体工程(保持不动, 当对照组)
├── docs/                       ← 单体工程的测试文档(01~08 + 缺陷清单)
├── tests/                      ← 测试资产(接口自动化 / 压测 / 产物)
└── blog-cloud/                 ← 微服务学习工程(本教程的主角)
    ├── docs/                   ← 微服务工程自己的文档(本教程就在这里)
    ├── pom.xml                 ← 父工程: 统一管理所有依赖版本
    ├── start-all.bat           ← 一键启动(双击)
    ├── stop-all.bat            ← 一键停止(双击)
    ├── init.sql                ← 建库脚本(3 张表)
    ├── maven-settings.xml      ← Maven 代理配置(本机专用, 可选)
    ├── blog-common/            ← 公共层: 常量/异常/工具/JWT/POJO(不含 Web, 网关也能用)
    ├── blog-web-common/        ← Web 公共层: 统一响应包装 + 全局异常
    ├── blog-user-service/      ← 用户服务  端口 8081
    ├── blog-blog-service/      ← 博客服务  端口 8082
    └── blog-gateway/           ← 网关      端口 8080(还托管前端页面)
```

> **为什么要拆两个 public 模块**:`blog-common` 里不放任何 Spring MVC 的类,
> 这样基于 WebFlux 的网关才能安全引用它(网关和 MVC 不能混在一个进程里);
> 需要 MVC 的"响应包装/异常处理"就放到 `blog-web-common`,只给两个业务服务用。

### 版本选型(为什么是这些版本)

| 依赖 | 版本 | 说明 |
|---|---|---|
| Spring Boot | 3.3.5 | 与单体工程保持一致 |
| Java | 17 | 本机用 JDK 21 编译,字节码目标 17 |
| Spring Cloud | 2023.0.3 | 管 Gateway / OpenFeign / LoadBalancer 的版本 |
| Spring Cloud Alibaba | 2023.0.3.2 | 管 Nacos / Sentinel / Seata / RocketMQ 的版本 |
| Nacos Server | 2.4.3 | 本机已装在 `D:\nacos-server-2.4.3\nacos` |
| RocketMQ | 5.2.x | 学 RocketMQ 时再装 |
| Seata Server | 2.1.0 | 学 Seata 时再装 |

**版本必须成套**:Spring Boot 3.3.x 只能配 Spring Cloud 2023.0.x + SCA 2023.0.3.x。
父 pom 里用两个 BOM 统一管住,子模块引依赖时**不写版本号**(写了反而容易冲突)。

> **BOM 是什么**:一份"依赖版本清单"。`<dependencyManagement>` 里 import 一个 BOM,
> 之后引它的组件就不用写版本号了。
> **注意坑**:SCA 的 BOM 坐标是 `com.alibaba.cloud:spring-cloud-alibaba-dependencies`
> (网上很多老教程写 `org.springframework.cloud`,那是 2020 年前的旧坐标,只有 0.x 版本)。

---

## 2. 问题 → 组件 对照表

先记住这张表,后面全是对它的展开:

| 组件(专业名词) | 它是干什么的(人话) | 本工程里用在哪 |
|---|---|---|
| **Nacos 注册中心** | 服务的**通讯录**:服务启动时来登记,别人按名字查地址 | 三个服务全部注册 |
| **Nacos 配置中心** | 配置的**公告板**:配置放在 Nacos,改完不用重启 | user-service 的 `app.switch-on` 热更新 |
| **OpenFeign** | 服务之间的**电话机**:写个接口就能调对方,不用写 HTTP | 博客服务调用户服务、用户服务调博客服务 |
| **Gateway 网关** | 系统的**大门 + 门卫**:所有请求从这里进,顺便查证件 | 8080 端口,统一鉴权 |
| **Sentinel 哨兵** | **闸机 + 保险丝**:挡住超额流量,对方挂了给兜底 | 列表限流、作者名熔断降级 |
| **Seata** | **分布式事务**:跨服务的写操作,要么都成功要么都回滚 | 发博客(写博客 + 改用户计数) |
| **RocketMQ** | **邮局**:把事件投递出去,别人异步来取 | 删博客后异步把用户计数 -1 |

> **分布式事务是什么**:一次业务操作要改多个服务的数据,必须保证"全成功"或"全失败",不能改一半。
> 就像转账:你这边扣钱、对方那边加钱,不能只成功一边。

---

## 3. 一条请求串起所有组件

**"发一篇博客"** 是本工程最完整的一条链路,几乎把所有组件都用上了。看懂这张图,你就懂了 80%:

```
浏览器 (blog_edit.html)
   │  1. POST /blog/add,请求头带 user_token
   ▼
┌─────────────────────────────────────────────────────────┐
│ 网关 blog-gateway :8080                                  │
│  2. AuthGlobalFilter 校验 JWT(令牌)✅                    │
│  3. 通过 → 往请求头塞 X-User-Id=1、X-User-Name=admin      │
│  4. 看路由:/blog/** → lb://blog-service                  │
│     lb = load balance(负载均衡),让 Nacos 帮忙找实例地址    │
└─────────────────────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────────────────────┐
│ 博客服务 blog-blog-service :8082                         │
│  5. @GlobalTransactional 开启全局事务(Seata)            │
│  6. 从请求头 X-User-Id 取出用户(不再自己解析 token)      │
│  7. 往 blog_info 表 insert 一条博客                       │
│  8. 调 userClient.increaseBlogCount(1)  ← Feign         │
└─────────────────────────────────────────────────────────┘
   │  Feign 拿服务名 "user-service" 去 Nacos 查地址
   ▼
┌─────────────────────────────────────────────────────────┐
│ 用户服务 blog-user-service :8081                         │
│  9. 执行 UPDATE user_info SET blog_count = blog_count+1  │
│ 10. 任何一步出错 → Seata 让第 7 步的 insert 一起回滚      │
└─────────────────────────────────────────────────────────┘
   │
   ▼  返回成功
```

**一句话记住**:请求从网关进 → 网关鉴权后转发给博客服务 → 博客服务用 Feign 叫用户服务 → Seata 保证两边一起成功。

---

## 4. 七个组件逐个讲

> 每节结构统一:**它解决什么问题 → 怎么用(代码) → 怎么验证有效果**。

### 4.1 Nacos 注册中心(通讯录)

**解决问题**:服务之间按**名字**找对方,而不是写死 IP 和端口。

**怎么用**——每个服务的 `application.yml` 加四行:

```yaml
spring:
  application:
    name: user-service            # 我的名字,别人就用这个名字找我
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        fail-fast: false          # Nacos 没启动也不许拦着我启动(踩过的坑,见第 9 节)
```

依赖只有一个:

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

**发生了什么**:服务启动后自动把自己的 IP + 端口登记到 Nacos;别人要调用时,拿着服务名去问 Nacos 要地址列表。

**怎么验证**:启动三个服务后,打开 <http://localhost:8848/nacos>(账号密码都是 `nacos`)→ 服务管理 → 服务列表,应该看到:

```
user-service      健康实例数 1
blog-service      健康实例数 1
blog-gateway      健康实例数 1
```

> **注册中心 / 服务发现是什么**:注册中心 = 一份"谁在哪"的名单(通讯录);服务发现 = 调用方按名字去这份名单里查地址。有了它,你把 user-service 换到别的端口、甚至开两台,调用方都不用改代码。

---

### 4.2 Nacos 配置中心(公告板)

**解决问题**:配置不想跟着 jar 包走,改配置不想重启服务。

**怎么用**——`application.yml` 里声明"去 Nacos 拉这份配置":

```yaml
spring:
  config:
    import: optional:nacos:user-service.yaml   # optional = Nacos 没有/连不上就用本地值兜底
```

再写一个普通类来接收配置:

```java
@Data
@Component
@ConfigurationProperties(prefix = "app")   // 对应 Nacos 里的 app.xxx
public class AppConfig {
    private String switchOn;               // 对应 app.switch-on
}
```

用的时候直接注入:

```java
@GetMapping("/configInfo")
public String configInfo() {
    return "app.switch-on = " + appConfig.getSwitchOn();
}
```

**怎么验证**:

1. Nacos 控制台 → 配置管理 → 新建配置:dataId = `user-service.yaml`,group = `DEFAULT_GROUP`,内容:
   ```yaml
   app:
     switch-on: closed
   ```
2. 浏览器访问 <http://localhost:8080/user/configInfo> → 值从 `open` 变成 `closed`
3. **全程没有重启服务** —— 这就是配置热更新

> **热更新是什么**:服务运行中,配置改了立刻生效,不用重启。原理是 Nacos 推送变更 → Spring Cloud 发一个刷新事件 → 重新绑定配置对象。

---

### 4.3 OpenFeign(服务之间的电话机)

**解决问题**:服务之间调用不想手写 HTTP 代码和 URL。

**第一步**:写一个"接口",声明你要调对方哪个接口(这是全部代码):

```java
@FeignClient(name = "user-service")               // 我要打给名字叫 user-service 的服务
public interface UserClient {

    @GetMapping("/user/internal/getUserInfo")     // 调它的这个接口
    UserInfoResponse getUserInfo(@RequestParam("userId") Integer userId);
}
```

**第二步**:启动类上开个开关:

```java
@SpringBootApplication(scanBasePackages = "com.example.blogcloud")
@EnableFeignClients(basePackages = "com.example.blogcloud.blog.client")   // 开启 Feign 客户端扫描
public class BlogServiceApplication { ... }
```

**第三步**:在业务代码里**像调本地方法一样**用它:

```java
@Autowired
private UserClient userClient;

public String getAuthorName(String userId) {
    UserInfoResponse user = userClient.getUserInfo(Integer.valueOf(userId));  // 实际发了 HTTP 请求
    return user == null ? "未知作者" : user.getUserName();
}
```

**你一行 HTTP 代码都没写、一个 URL 都没拼、负载均衡也没管** —— 这就是 Spring Cloud 的价值:Nacos 给地址,LoadBalancer 挑一台,Feign 负责发请求和把 JSON 变成对象。

> **OpenFeign 是什么**:一个"声明式 HTTP 客户端"。你只写接口 + 注解,它动态生成实现类帮你发请求。
> **负载均衡是什么**:同一个服务开了多个实例时,"挑哪一台"的策略(默认轮询:这次 1 号、下次 2 号)。

**⚠️ 这条规则一定要记住(我踩过坑)**:服务之间调用,必须调**内部接口**(路径带 `/internal/` 的)。

原因是本工程有个"统一响应包装",会把 Controller 的返回值包成 `{"code":200,"data":{...}}`。
Feign 如果去调这种被包了壳的公开接口,它按 `UserInfoResponse` 反序列化时会发现字段对不上,**结果全是 null**(博客详情的作者名一开始就因此显示为空)。

所以内部接口的写法是:

```java
/**
 * 内部接口: 查询用户信息(供 blog-service 的 Feign 调用)
 * 路径含 /internal/ 的接口不套 Result 壳(见 ResponseAdvice)
 */
@GetMapping("/internal/getUserInfo")
public UserInfoResponse getUserInfoInternal(@RequestParam @NotNull Integer userId) {
    return userService.getUserInfo(userId);
}
```

而 `ResponseAdvice` 里加了这么一句"对内部接口放行":

```java
// 内部接口(服务间调用)直接返回原始对象, 不套 Result 壳
if (request.getURI().getPath().contains("/internal/")) {
    return body;
}
```

---

### 4.4 Gateway 网关(大门 + 门卫)

**解决问题**:请求统一入口、鉴权只写一遍、前端不用记一堆端口。

**第一件事:配路由**(纯 yaml,一行行读就懂):

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user_service
          uri: lb://user-service          # lb = load balance,交给 Nacos 解析真实地址
          predicates:
            - Path=/user/**               # 什么样的请求交给它
        - id: blog_service
          uri: lb://blog-service
          predicates:
            - Path=/blog/**
```

**第二件事:写一个全局过滤器做鉴权**(`AuthGlobalFilter`),逻辑就三步:

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // 1) 白名单(登录/注册/静态页面)直接放行
    if (!isBusiness(path) || isWhitelist(path)) {
        return chain.filter(exchange);
    }

    // 2) 其他业务接口校验 JWT(令牌),无效返回 401
    String token = exchange.getRequest().getHeaders().getFirst(Constants.TOKEN);
    Claims claims = JwtUtils.parseJwt(token);
    if (claims == null) {
        return unauthorized(exchange);      // 返回 {"code":-1,"errMsg":"未登录或登录已失效"}
    }

    // 3) 校验通过 → 把用户身份塞进请求头,转发给下游服务
    ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(Constants.HEADER_USER_ID, String.valueOf(JwtUtils.getUserIdFromToken(token)))
            .header(Constants.HEADER_USER_NAME, JwtUtils.getUserNameFromToken(token))
            .build();
    return chain.filter(exchange.mutate().request(mutated).build());
}
```

**下游服务怎么拿当前用户**?直接读请求头,不用再解析 token 了:

```java
private Integer getUserId() {
    String headerUserId = request.getHeader(Constants.HEADER_USER_ID);   // 网关注入的
    if (headerUserId != null && !headerUserId.isEmpty()) {
        return Integer.valueOf(headerUserId);
    }
    // 兜底: 绕过网关直接调试时, 回退到解析 token
    ...
}
```

**顺带的好处**:前端 6 个 HTML 页面直接放在网关的 `static/` 目录里托管,所以浏览器**只需要认 8080 这一个地址**。

> **网关是什么**:系统的唯一入口。所有外部请求先到它这里,由它决定"转给谁""要不要拦"。
> **路由 / 断言(predicate)是什么**:路由 = "什么样的请求 → 转给哪个服务"的规则;断言 = 判断条件(这里是按路径前缀判断)。
> **全局过滤器是什么**:所有请求都要经过的一段代码,常用来做鉴权、日志、跨域。

**怎么验证**:不带 token 访问 `http://localhost:8080/blog/getList` → 返回 401;带上登录拿到的 token 再访问 → 正常返回列表。

---

### 4.5 Sentinel(闸机 + 保险丝)

**解决问题**:接口被刷爆要有"限流";某个服务挂了不能把整条链路拖死,要有"熔断 + 兜底"。

**玩法一:服务端限流**(用代码写规则,最直观):

```java
// GET:/blog/getList 这个接口,每秒最多 1 次
FlowRule flowRule = new FlowRule();
flowRule.setResource("GET:/blog/getList");        // 资源名: 接口就是一个资源
flowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);   // QPS = 每秒请求数
flowRule.setCount(1);
FlowRuleManager.loadRules(List.of(flowRule));
```

**玩法二:熔断 + 兜底**。先给方法打标:

```java
@SentinelResource(value = "getAuthorName", fallback = "getAuthorNameFallback")
public String getAuthorName(String userId) {
    UserInfoResponse user = userClient.getUserInfo(Integer.valueOf(userId));  // 这里可能失败
    return user == null ? "未知作者" : user.getUserName();
}

/** 兜底方法: 参数与原方法一致, 末尾多一个 Throwable */
public String getAuthorNameFallback(String userId, Throwable throwable) {
    log.warn("获取作者名失败, 触发 Sentinel 兜底: {}", throwable.getMessage());
    return "未知作者";
}
```

再配熔断规则(异常太多就"跳闸",一段时间内直接走兜底):

```java
DegradeRule degradeRule = new DegradeRule("getAuthorName");
degradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_COUNT);  // 按异常数熔断
degradeRule.setCount(3);        // 1 秒内异常 >= 3 次
degradeRule.setTimeWindow(5);   // 熔断 5 秒
DegradeRuleManager.loadRules(List.of(degradeRule));
```

**玩法三:网关限流**(在入口挡住,更省事):

```java
GatewayFlowRule rule = new GatewayFlowRule("blog_service");  // 路由 id
rule.setCount(2);            // 每秒 2 次
rule.setIntervalSec(1);
GatewayRuleManager.loadRules(Collections.singleton(rule));
```

**怎么验证**:
- 快速刷新博客列表(1 秒超过 1 次)→ 出现 `Blocked by Sentinel (flow limiting)`
- **把用户服务停掉,再刷新博客详情页** → 页面不报 500,作者名显示"未知作者"(兜底生效)

> **限流是什么**:给接口设个"每秒最多几次"的上限,超了就拒绝,保护系统不被打垮。
> **熔断是什么**:像家里的保险丝。下游服务连续失败到一定次数,就"跳闸"一段时间,这期间不再去调用它(直接走兜底),避免请求堆积把调用方也拖死。
> **降级/兜底是什么**:主逻辑不可用时,返回一个"还能用"的替代结果(比如"未知作者"),而不是抛异常给用户看。

---

### 4.6 Seata(分布式事务)

**解决问题**:一次业务要改两个服务的数据,必须"要么都成功,要么都回滚"。

**核心只有一个注解**,加在**调用链最外层的入口方法**上:

```java
@Override
@GlobalTransactional(name = "blog-add-transaction", rollbackFor = Exception.class)
public void addBlog(String title, String content, Integer userId) {
    // 分支 1: 写博客自己的库
    blogInfoMapper.insert(blogInfo);

    // 分支 2: 远程调用用户服务, 改用户库
    userClient.increaseBlogCount(userId);
}
```

**配套要做的两件事**:

1. 两个服务配同一个"事务组"(这样它们才知道属于同一次全局事务):

```yaml
seata:
  enabled: false                     # 学习工程默认关闭, 演示时改成 true
  application-id: blog-service
  tx-service-group: blog_tx_group    # 两个服务必须写一样的组名
  registry:
    type: file
  service:
    vgroup-mapping:
      blog_tx_group: default
    grouplist:
      default: 127.0.0.1:8091        # Seata Server 地址
```

2. 数据库里建一张 `undo_log` 表(见 `blog-cloud/init.sql`,本文档所在目录的上一级)—— Seata 靠它记录"数据改之前长什么样",回滚时照着还原。

**怎么验证(最有说服力的一步)**:

| 场景 | 操作 | 预期 |
|---|---|---|
| 正常 | 发一篇博客 | 博客出现在列表里,**且**作者 `blog_count` +1 |
| 回滚 | **先停掉用户服务**,再发博客 | 接口报错,**且博客没有落库**(整体回滚) |
| 对比 | 关闭 `seata.enabled` 重复上一步 | 博客**会**写进去但远程调用失败(数据不一致)—— 这就是没有分布式事务的后果 |

> **事务是什么**:一组操作,要么全做完,要么全不做。本地事务(`@Transactional`)只能管一个数据库。
> **分布式事务是什么**:跨多个服务/多个数据库的事务,由 Seata 这样的框架统一协调。
> **XID 是什么**:全局事务的唯一编号。Seata 开启事务时生成一个 XID,通过 Feign 请求头一路传给下游服务,大家凭这个编号"认领"自己属于哪次事务。
> **AT 模式是什么**:Seata 最常用的模式,业务代码不用改,靠 `undo_log` 自动生成反向 SQL 来回滚。

---

### 4.7 RocketMQ(邮局)

**解决问题**:有些事情不需要用户等着,发个消息让别的服务慢慢处理就行。

**场景**:删除博客后,把作者的博客数 -1。这件事不需要用户等待,适合异步。

**消费端**(用户服务)—— 就是一个 Bean:

```java
@Bean
public Consumer<BlogPublishMessage> blogIn() {     // 方法名 blogIn ↔ 配置里 blog-in-0
    return message -> {
        log.info("【RocketMQ】消费博客事件: blogId={}, userId={}",
                 message.getBlogId(), message.getUserId());
        // 把作者博客数 -1(顺手防止减成负数)
        UpdateWrapper<UserInfo> uw = new UpdateWrapper<>();
        uw.eq("id", message.getUserId())
          .setSql("blog_count = IF(blog_count > 0, blog_count - 1, 0)");
        userInfoMapper.update(null, uw);
    };
}
```

**生产端**(博客服务)—— 一行发送:

```java
if (mqEnabled) {   // 配置开关, 见下面
    streamBridge.send("blog-out-0", new BlogPublishMessage(blogId, title, userId));
}
```

**配置文件**(只在激活 `mq` profile 时生效):

```yaml
spring:
  cloud:
    stream:
      rocketmq:
        binder:
          name-server: 127.0.0.1:9876    # RocketMQ 地址
      bindings:
        blog-out-0:
          destination: blog-topic        # 主题名: 生产者和消费者靠它对上
          content-type: application/json
```

**一个贴心设计**:消费端 Bean 上加了 `@Profile("mq")`:

```java
@Configuration
@Profile("mq")     // 只有加了 --spring.profiles.active=mq 才创建这个消费者
public class RocketMqConsumerConfig { ... }
```

这样**没装 RocketMQ 时服务照样能启动**,不会因为连不上消息队列就起不来。

**怎么验证**:启动 RocketMQ → 两个服务都加 `--spring.profiles.active=mq` 重启 → 删一篇博客 → 看用户服务日志里出现"消费博客事件",数据库 `blog_count` -1。

> **消息队列(MQ)是什么**:一个"中间存储 + 转发"的邮局。发送方把消息丢进去就走(不用等),接收方有空了再来取。
> **生产者 / 消费者是什么**:发消息的叫生产者,收消息的叫消费者。
> **Topic(主题)是什么**:消息的分类邮箱,生产者往 `blog-topic` 发,消费者从 `blog-topic` 取。
> **profile 是什么**:Spring Boot 的"配置开关"。`--spring.profiles.active=mq` 就是打开名为 mq 的那套配置。

---

## 5. 单体写法 → 微服务写法

这张表帮你把新旧知识挂上钩:

| 你以前(单体) | 现在(微服务) | 本质变化 |
|---|---|---|
| `userService.getUserInfo(1)` | `userClient.getUserInfo(1)` | 本地方法 → 网络调用(Feign 藏起来了) |
| `@Transactional` | `@GlobalTransactional` | 单库事务 → 跨服务事务(Seata) |
| `LoginInterceptor` 拦截器 | 网关 `AuthGlobalFilter` | 每个服务各拦 → 入口统一拦一次 |
| `application.yml` 改完重启 | Nacos 配置中心热更新 | 配置跟包走 → 配置在外部 |
| 手写 `try/catch` 兜底 | `@SentinelResource(fallback=...)` | 手写容错 → 框架统一容错 |
| 直接 join 另一张表 | 调另一个服务的接口 | 共享数据库 → 数据各归各的服务 |

**最后一行是微服务最核心的纪律**:博客服务**不允许**直接查 `user_info` 表,只能通过接口问用户服务要数据。
本工程里连"用户服务要根据博客 id 查作者"这种需求,也是老老实实 Feign 调 `/blog/internal/getBlogInfo`。

---

## 6. 术语小词典

| 名词 | 人话解释 |
|---|---|
| 微服务 | 把一个大应用拆成多个能独立运行、独立部署的小服务 |
| 单体 | 所有功能都在一个进程里的应用(你原来的 BlogSystem) |
| 注册中心 | 记录"哪个服务在哪个地址"的名单 |
| 服务发现 | 调用方按服务名去注册中心查地址的过程 |
| 心跳 / 健康实例 | 服务定期向注册中心报到;超时没报到就被标记为不健康、不再被调用 |
| 负载均衡 | 一个服务有多个实例时,挑一台来处理的策略 |
| 网关 | 系统的统一入口,负责路由和鉴权 |
| 路由 / 断言 | "什么请求转给谁"的规则 / 规则的判断条件 |
| 全局过滤器 | 所有请求都要经过的一段代码 |
| 限流 | 给接口设访问速率上限 |
| 熔断 | 下游连续失败时"跳闸",一段时间内不再调用它 |
| 降级 / 兜底 | 主逻辑不可用时返回一个可用的替代结果 |
| 分布式事务 | 跨多个服务/数据库的事务,保证全成功或全回滚 |
| XID | 全局事务的唯一编号,跨服务传递 |
| undo_log | Seata 的回滚日志表,记录数据修改前的样子 |
| 消息队列 / MQ | 异步传递消息的中间件,发送方不用等接收方 |
| Topic | 消息的分类,生产者发到这里,消费者从这里取 |
| 配置中心 | 集中存放配置的地方,支持改完不重启生效 |
| 热更新 | 运行中改配置立即生效 |
| starter | Spring Boot 的一揽子依赖包,引一个就配好一堆东西 |
| BOM | 统一管理依赖版本的清单(本工程在父 pom 里统一管 Spring Cloud 版本) |
| profile | Spring Boot 的配置分组开关,如 `mq` |
| JWT / token | 登录后服务器发的一张"电子通行证",后续请求带着它证明身份 |
| Feign 客户端 | 用接口 + 注解声明的"远程调用代理" |

---

## 7. 把项目跑起来

### 前置条件(本机已就绪)

- MySQL 8 已启动,`blog_cloud` 库已建好(三张表:`user_info` / `blog_info` / `undo_log`)
- Nacos 已安装在 `D:\nacos-server-2.4.3\nacos`
- 代码已构建(`blog-cloud` 各模块 `target/*.jar` 存在)

### 7.1 方式 A:一键脚本(最省事,推荐)

打开 `D:\java\BlogSystem\blog-cloud\`,**双击 `start-all.bat`**,等约 40 秒(冷启动)。

它会自动:检查 jar → 没运行就启动 Nacos 并等它就绪 → 依次启动 用户服务(8081)→ 博客服务(8082)→ 网关(8080)→ 打印状态表 → 打开浏览器。

**预期看到最后一段**:

```
==========================================================
                      启动结果
==========================================================
   Nacos      端口 8848   运行中 OK
   用户服务   端口 8081   运行中 OK
   博客服务   端口 8082   运行中 OK
   网关       端口 8080   运行中 OK

   浏览器访问: http://localhost:8080/blog_list.html
   管理员账号: admin / admin123
```

| 想做什么 | 双击哪个 |
|---|---|
| 启动全部 | `start-all.bat` |
| 停止三个服务(保留 Nacos) | `stop-all.bat` |
| 服务 + Nacos 全停 | `stop-all.bat all` |

> 脚本跑的是**打包好的 jar**。改了代码要先重新构建:`mvn -o -DskipTests clean package`,
> 否则跑的还是旧代码(脚本会检测并提醒你)。

### 7.2 方式 B:在 IDEA 里启动(适合边看代码边调试)

1. IDEA → `File` → `Open` → 选择 **`D:\java\BlogSystem\blog-cloud`**(注意是 blog-cloud)
2. 依次打开并 Run 三个启动类(点 `main` 方法左边的绿色三角):

| 顺序 | 文件 | 成功标志 |
|---|---|---|
| 1️⃣ | `blog-user-service/.../user/UserServiceApplication.java` | `Tomcat started on port 8081` |
| 2️⃣ | `blog-blog-service/.../blog/BlogServiceApplication.java` | `Tomcat started on port 8082` |
| 3️⃣ | `blog-gateway/.../gateway/GatewayApplication.java` | `Tomcat started on port 8080` |

> Nacos 要先启动:双击 `D:\nacos-server-2.4.3\nacos\bin\startup.cmd`(必须带 `-m standalone` 参数时用命令行)。
> 用 IDEA 启动前,先 `stop-all.bat` 停掉脚本起的服务,避免端口冲突。

### 7.3 打开页面开始玩

浏览器访问 <http://localhost:8080/blog_list.html> → 注册 → 登录 → 发博客 → 看详情 → 编辑 → 删除。

用 `admin / admin123` 登录还能进管理页,体验管理员上下架。

### 7.4 方式 C:纯命令行(不用 IDEA、不用脚本)

已经构建过 jar 的话,开三个命令行窗口分别执行:

```cmd
cd /d D:\java\BlogSystem\blog-cloud
java -jar blog-user-service\target\blog-user-service-0.0.1-SNAPSHOT.jar

:: 另开一个窗口
cd /d D:\java\BlogSystem\blog-cloud
java -jar blog-blog-service\target\blog-blog-service-0.0.1-SNAPSHOT.jar

:: 再开一个窗口
cd /d D:\java\BlogSystem\blog-cloud
java -jar blog-gateway\target\blog-gateway-0.0.1-SNAPSHOT.jar
```

重新构建(依赖已缓存,离线也能打包):

```cmd
cd /d D:\java\BlogSystem\blog-cloud
mvn -o -DskipTests clean package
```

首次建库(只需一次):

```cmd
"D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -proot -e "source D:/java/BlogSystem/blog-cloud/init.sql"
```

---

## 8. 验收清单:怎么确认组件真的生效了

每一条都是"操作 → 预期结果",打勾即验收完成。

### 阶段一:不需要任何中间件也能验

- [ ] 三个服务能启动(控制台出现 `Started xxxApplication`)
- [ ] 直连 8081 注册/登录成功;直连 8082 能发博客(请求头带 `user_token`)
- [ ] 停掉用户服务再查博客详情 → `authorName` 变成"未知作者"(Sentinel 兜底,不是 500)

### 阶段二:Nacos 注册中心 + 配置中心

- [ ] Nacos 控制台服务列表能看到 user-service / blog-service / blog-gateway
- [ ] 带 token 经网关访问 `/blog/getList` → 200(说明网关 → Nacos → 服务这条链路通了)
- [ ] Nacos 新建 `user-service.yaml` 写 `app.switch-on: closed` → `/user/configInfo` 不重启就变 `closed`
- [ ] 把值改回 `open` → 又变回来
  > 注意:直接**删除**配置不会回退成本地值,而是保留最后一次的值(实测行为)

### 阶段三:Gateway 鉴权 + 前端全流程

- [ ] 无 token 访问 `/blog/getList` → **401**
- [ ] 伪造/改坏 token → **401**
- [ ] 带真 token → 200
- [ ] 浏览器走完:注册 → 登录 → 发博客 → 详情(作者名正确)→ 编辑 → 删除 → admin 进管理页上下架

### 阶段四:Sentinel

- [ ] 1 秒内多次请求 `/blog/getList` → 出现 `Blocked by Sentinel (flow limiting)`
- [ ] 停掉用户服务,连刷博客详情 → 前几次失败后进入熔断,稳定返回"未知作者"
- [ ] 快速刷新列表(>2 次/秒)→ 网关返回 `请求过于频繁, 已被 Sentinel 网关限流`

### 阶段五:RocketMQ(需要先启动 RocketMQ)

- [ ] 两个服务加 `--spring.profiles.active=mq` 重启,日志无连接报错
- [ ] 删除一篇博客 → 博客服务日志出现"发送博客删除事件",用户服务日志出现"消费博客事件",`blog_count` -1
- [ ] (可选)先停用户服务再删博客 → 再启动用户服务 → 消息被补消费(消息没丢)

### 阶段六:Seata(需要先启动 Seata Server)

- [ ] 两个服务 `seata.enabled` 改 `true` 重启,日志显示注册 RM/TM 成功
- [ ] 正常发博客 → 博客落库 **且** `blog_count` +1
- [ ] **停掉用户服务再发博客 → 接口报错且博客不落库(整体回滚)**
- [ ] 对照:把 `seata.enabled` 改回 `false` 重复上一步 → 博客会落库但报错(数据不一致)

---

## 9. 常见问题急救表

| 现象 | 原因 / 解决 |
|---|---|
| 服务启动报 Nacos 注册失败并直接退出 | 新版 SCA 默认 fail-fast=true 会中断启动;本工程已在三个服务里显式设 `spring.cloud.nacos.discovery.fail-fast: false`,未起 Nacos 也只告警。起好 Nacos 后重启服务即可注册 |
| 网关访问业务接口返回 503 | 下游服务没注册到 Nacos(没启动 / Nacos 没起)。先确认控制台能看到服务 |
| 网关返回 401 | token 缺失或过期 → 先调用 `/user/login` 拿新 token,请求头带 `user_token` |
| 发博客报 `user-service executing POST ...` | 用户服务不可达(没起 Nacos / 用户服务没启动)。发博客是跨服务写操作,本来就依赖对方;开启 Seata 后该失败会与博客写入一起回滚 |
| 博客详情 `authorName` 为空 | Feign 调了被 `Result` 包壳的公开接口 → 服务间调用必须走 `/internal/` 接口 |
| 发博客后 `blog_count` 没变 | Seata 未开启(默认关闭)时该计数不维护;或开启后 Seata Server 没起导致回滚 |
| 删博客后 `blog_count` 没减 | 没加 `mq` profile,或 RocketMQ 没启动 |
| 改 Nacos 配置不生效 | dataId 必须是 `user-service.yaml`,group 必须是 `DEFAULT_GROUP`,内容要有 `app.switch-on` |
| Nacos 起不来 / 报集群错误 | 必须用 `-m standalone` 启动 |
| 端口 8080/8081/8082 被占用 | 有旧进程没关:双击 `stop-all.bat`(会提示;实在不行重启电脑) |
| `mvn` 下载依赖失败 | 本机 HTTPS 直连中央仓库不通,用 `-s maven-settings.xml` 走代理;依赖已缓存时可用 `mvn -o` 离线构建 |
| 中文在命令行里显示成乱码 | 只是命令行编码问题,数据库和网页里是正常的(实测确认过) |
| YAML 里写 `switch-on: on` 读出来变成 `true` | YAML 1.1 会把 `on/off/yes/no` 当布尔值 → 改用 `open/closed` 这类普通字符串 |

---

## 10. 只记五句话

1. **Nacos** = 通讯录 + 公告板:服务来报到,配置集中放
2. **Gateway** = 唯一大门:路由 + 鉴权,只在这一处做
3. **Feign** = 电话机:用服务名调别的服务,不写 IP
4. **Sentinel** = 闸机 + 保险丝:挡住过量请求,对方挂了给兜底
5. **Seata** = 对公转账:跨服务改数据,要么全成要么全回滚

(第六句:**RocketMQ** = 邮局,寄出去就不管了,对方有空再来取。)

---

## 11. 下一步学什么 / 该看哪几个文件

### 想深入时,按这个顺序读代码(4 个文件就通)

| 顺序 | 文件 | 看什么 |
|---|---|---|
| 1 | `blog-gateway/.../filter/AuthGlobalFilter.java` | 网关怎么鉴权、怎么把用户身份传给下游 |
| 2 | `blog-blog-service/.../controller/BlogController.java` | 下游服务怎么用网关注入的请求头 |
| 3 | `blog-blog-service/.../service/impl/BlogServiceImpl.java` | Seata 全局事务 + Feign 远程调用的交叉点 |
| 4 | `blog-blog-service/.../service/AuthorInfoService.java` | Sentinel 兜底(熔断降级)怎么写 |

### 可以动手改的小练习

1. 把 `SentinelConfig` 里的限流 QPS 从 1 改成 5,重启观察变化
2. 在 Nacos 的 `user-service.yaml` 里加一个 `app.title`,给 `/user/configInfo` 加个字段返回
3. 把 RocketMQ 的场景换个触发点(比如"发博客"也发消息),体会事件驱动的设计
4. 开启 Seata 后看日志里 XID 是怎么跨服务传递的

### 之后可以加的技术(建议顺序)

- **Redis**:缓存博客列表/详情(缓存)、把 token 存 Redis 实现"主动踢人"、用 Redisson 做分布式锁
- **Sentinel Dashboard**:把代码里的规则改成控制台动态推送
- **链路追踪**(Micrometer Tracing):给一次请求打上 traceId,跨服务排查慢在哪
- **接口级自动化测试**:基于 RestAssured + Testcontainers,把验收清单里的操作变成自动化用例
