# 3-Layer Microservices Distributed Tracing (Micrometer Tracing + Zipkin)

本项目是一个基于 **Spring Boot 3.5.0** 和 **JDK 25** 的 3 层微服务架构示例（`Gateway` -> `Service A` -> `Service B`），展示了如何利用 **Micrometer Tracing**、**Brave** 和 **Zipkin** 实现端到端分布式全链路追踪、线程池异步 TraceId 传递以及远程 OpenFeign 传参。

---

## 🛠️ 技术栈与版本说明

| 技术组件 | 采用版本 | 职责与作用 |
| :--- | :--- | :--- |
| **JDK** | 25 | 基础运行时环境，使用最新 Java 特性与 Javac 编译 |
| **Spring Boot** | 3.5.0 | 微服务应用基石，Spring Boot 3.x 引入了全新的观察性（Observability）设计 |
| **Spring Cloud** | 2025.0.0 | 微服务开发套件管理（Feign, LoadBalancer 等） |
| **Spring Cloud Alibaba** | 2025.0.0.0 | Nacos 注册与发现组件适配支持 |
| **Nacos** | 2.2.3 (Docker) | 服务注册中心，用于微服务发现与动态感知 |
| **Micrometer Tracing** | - | 门面（Facade）级链路追踪 API，替代原 Spring Cloud Sleuth |
| **Brave** | - | 链路追踪的具体内核引擎，提供 TraceContext 管理与 Span 生命周期维护 |
| **Zipkin Reporter** | - | 异步链路数据上报组件，将收集到的 spans 投递至 Zipkin Collector |
| **Zipkin** | - (Docker) | 全链路数据存储与可视化分析控制台 (Port: 9411) |

---

## 🧠 链路追踪组件核心实现原理

在 Spring Boot 3.x 体系中，原先由 Spring Cloud Sleuth 承担的链路追踪功能被正式移交给了 **Micrometer Tracing** 项目。下面是本项目所集成的三大核心组件的协作原理与实现细节：

```
+-------------------------------------------------------------------------------+
|                                 Your Application                              |
|   (e.g., Spring Web MVC / Controller / OpenFeign / @Async Asynchronous Task)  |
+-------------------------------------------------------------------------------+
                                        |
                                        v (API Calls)
+-------------------------------------------------------------------------------+
|                              Micrometer Tracing                               |
|        - Facade API (Tracer, Span, Span.Builder, TraceContext, Propagator)     |
+-------------------------------------------------------------------------------+
                                        |
                                        v (Bridges To)
+-------------------------------------------------------------------------------+
|                            Brave Tracing Engine                               |
|   - Real Tracer (Manages ThreadLocal span context, generates IDs)            |
|   - Propagation (Formats headers: W3C traceparent / B3)                       |
+-------------------------------------------------------------------------------+
                                        |
                                        v (Spans Finished)
+-------------------------------------------------------------------------------+
|                               Zipkin Reporter                                 |
|      - Non-blocking Memory Buffer (Queues finished spans)                     |
|      - Asynchronous Sender (HTTP POST to /api/v2/spans)                       |
+-------------------------------------------------------------------------------+
                                        |
                                        v (Network Upload)
+-------------------------------------------------------------------------------+
|                                 Zipkin Server                                 |
+-------------------------------------------------------------------------------+
```

### 1. Micrometer Tracing (门面 API 层)
* **实现原理**：
  Micrometer Tracing 类似于日志领域中的 **SLF4J**，它本身只是一套抽象门面 API（Facade），不提供链路追踪的底层实现。它定义了 `Tracer`、`Span`、`TraceContext` 等标准规范。
* **主要作用**：
  - 解耦业务代码与具体的链路追踪厂商实现。
  - 通过 Spring Boot 自动装配拦截并代理核心 HTTP 组件（如 Spring MVC Filter, Reactive WebClient, Spring Cloud Gateway 等），在请求发起与响应时自动进行 Span 生命周期的创建（Start/End）。

### 2. Brave Bridge (追踪引擎核心实现)
* **实现原理**：
  `micrometer-tracing-bridge-brave` 是一个桥接器，它将 Micrometer Tracing 的门面调用路由至具体的 **Brave** 引擎。
* **主要作用**：
  - **标识生成**：当创建一个新 Trace 或子 Span 时，Brave 负责生成唯一的全局 `TraceId`（通常为 64位 或 128位）以及当前节点的 `SpanId`。
  - **上下文管理**：Brave 使用 `ThreadLocal`（如 `ThreadLocalCurrentTraceContext`）来维护当前线程所处的链路上下文，确保在同一个线程内打印的日志能够通过 MDC 自动带上当前的 TraceId 和 SpanId。
  - **协议透传（Propagation）**：在进行跨服务网络调用（如 OpenFeign, RestTemplate）时，Brave 负责按照特定标准（如 B3 协议或 W3C Trace Context 规范的 `traceparent` 头）将当前 Trace 上下文序列化进 HTTP 请求头；接收端再从 Header 中反序列化出上下文，实现跨服务链路的串联。

### 3. Zipkin Reporter (异步链路数据上报器)
* **实现原理**：
  `zipkin-reporter-brave` 负责收集已经执行完毕（即调用了 `span.end()` 或 `span.finish()`）的 Span 数据。
* **主要作用**：
  - **非阻塞上报（Non-blocking Sending）**：如果每次请求完都同步向 Zipkin 发送请求，会严重拖慢微服务的响应时间。Zipkin Reporter 内部实现了一个**内存缓冲区（MpscQueue/BoundedQueue）**。当 Span 结束时，它会被丢入缓冲区，工作线程在后台批量、异步地将 span 数据打包，通过 HTTP 发送到 Zipkin 的 `/api/v2/spans` 接口。
  - **丢弃策略**：如果 Zipkin 服务器出现网络抖动或宕机，缓冲区满后上报器会根据配置策略丢弃多余的 spans，确保不因为链路监控导致应用 OOM。

---

## 🚀 项目结构与验证说明

项目骨架如下：
- `cloud-gateway`: 网关服务 (Port: 8080)
- `service-provider-a`: 中间服务 A (Port: 8081)
- `service-provider-b`: 终端业务服务 B (Port: 8082)

验证链路追踪是否正常运行：
1. **启动基础设施** (MySQL, Nacos, Zipkin):
   ```bash
   docker compose -f /path/to/docker-compose.yml up -d
   ```
2. **构建微服务**:
   ```bash
   mvn clean package -DskipTests
   ```
3. **依次启动网关、A、B 服务**，然后执行接口调用:
   ```bash
   curl.exe -i http://localhost:8080/service-a/test
   ```
4. 打开 **Zipkin UI** (`http://localhost:9411`)，运行查询即可看到完整的分布式服务树状拓扑及性能耗时。
