你是一个精通微服务架构的资深 Java 专家。请基于以下最新的技术栈，为我编写一个全链路追踪（Micrometer Tracing + Zipkin）的完整开发方案与核心代码。

【技术栈与版本要求】
- 核心框架：Spring Boot 3.5.0 + JDK25 D:\software\jdk-25
- 微服务治理：Spring Cloud Alibaba 2025.0.0.0 (支持 Spring Boot 3.5.x)
- 注册与配置中心：Nacos (搭配 spring-cloud-starter-alibaba-nacos-discovery)
- 负载均衡：Spring Cloud LoadBalancer (替代已废弃的 Ribbon)
- 链路追踪抽象：Micrometer Tracing
- 桥接层与上报：Micrometer Tracing Bridge Brave + Zipkin Reporter Brave

【链路调用场景要求】
必须包含一个完整的 3 层服务调用案例（Gateway -> Service-A -> Service-B），来演示 TraceId 在分布式环境下的无缝透传：
1. 服务网关 (cloud-gateway): 作为入口，负责接收请求、开启分布式链路，并通过 Spring Cloud Gateway 将请求路由到 Service-A。
2. 业务服务 A (service-provider-a): 接收网关请求，打印日志（包含 traceId），并使用 OpenFeign 远程调用 Service-B。
3. 业务服务 B (service-provider-b): 最终接收端，执行具体的业务逻辑（例如模拟数据库查询），打印业务日志。

请提供以下内容的详细实现：

1. 根项目的 Maven 依赖管理 (pom.xml)：
   - 请给出包含 Spring Boot 3.5.0、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0 依赖管理（dependencyManagement）的配置。
   - 给出服务中需要引入的 Micrometer Tracing、Brave 桥接器、Zipkin 上报器、OpenFeign 和 LoadBalancer 的精确依赖。

2. 统一的应用配置 (application.yml)：
   - 提供符合 Spring Boot 3.x 规范的 management.tracing 核心配置。
   - 设置采样率为 1.0（全量采样），并正确配置 management.zipkin.tracing.endpoint。
   - 配置统一的 logging.pattern.level，使网关和各服务控制台日志均能自动打印出 [应用名, traceId, spanId]。

3. 核心代码实现：
   - 编写 cloud-gateway 的路由配置及核心启动类。
   - 编写 service-provider-a 的 OpenFeign 客户端接口和 Controller。由于 Spring Boot 3.x 下 Feign 默认不透传 Micrometer 上下文，请务必给出让 Feign 能够传递 Trace 信息的配置或拦截器实现。
   - 编写 service-provider-b 的 Controller。并在代码中演示如何通过 ObservationRegistry 或 Tracer API 手动获取当前的 TraceId，封装进业务 R 对象中返回。

4. 线程池异步传递（最佳实践）：
   - 演示在 service-provider-a 中如果使用自定义线程池（ThreadPoolTaskExecutor）或 @Async 异步调用，应该如何配置 Micrometer 的 ContextSnapshot / ContextPropagatingTaskDecorator，以防止异步线程中 TraceId 丢失。

所有配置文件和 Java 代码请保持连贯、无拼写错误，采用标准的 Spring Boot 3 编码风格，并提供完整的、可直接复制的代码块。