#### Spring Cloud的各个组件的简介

1. Spring Cloud Config：配置管理，允许被集中化放到远程服务器中。目前支持本地存储、Git和SVN等
2. Spring Cloud Bus：分布式事件、消息总线、用于集群（如配置发生变化）中传播事件状态，可以与Spring Cloud Config联合实现热部署
3. Netflix Eureka：服务治理中心，它提供微服务的治理，包括微服务的注册和发现，是Spring Cloud的核心组件
4. Netflix Hystrix：断路器，在某个组件因为某些原因无法响应或者响应超时之际进行熔断，以避免其他微服务调用该组件造成大量线程积压。它提供了更为强大的容错能力
5. Netflix Zuul：API网关，它可以拦截Spring Cloud的请求，提供动态路由功能。它还可以限流，保护微服务持续可用，还可以通过过滤器提供验证安全
6. Spring Cloud Security：它是基于SpringSecurity的，可以给微服务提供安全控制
7. Spring Cloud Sleuth：它是一个日志收集工具包，可以提供分布式追踪的功能。它封装了Dapper和log-based追踪以及Zipkin和HTrace操作
8. Spring Cloud Stream：分布式数据流操作，它封装了关于Redis、RabbitMQ、Kafka等数据流的开发工具
9. Netflix Ribbon：提供客户端的负载均衡。它提供了多种负载均衡的方案，我们可以根据自己的需要选择某种方案。它还可以配合服务发现和断路器使用
10. Netflix Turbine：Turbine是聚合服务器发送事件流数据的工具，用来监控集群下Hystrix的metrics情况
11. OpenFeign：它是一个声明式的调用方案，可以屏蔽REST风格的代码调用，而采用接口声明方式调用，这样就可以有效减少不必要的代码，进而提高代码的可读性。
12. Spring Cloud Task：微服务的任务计划管理和任务调度方案

#### Spring Cloud 涉及的常用工具

1. 服务治理和服务发现（Spring CloudNetflix Eureka）
2. 服务调用（Spring Cloud Netflix Ribbon和Spring Cloud Netflix OpenFeign）
3. 断路器（Spring Cloud Netflix Hystrix和Resilience4j）
4. 网关（Spring Cloud Netflix Zuul和SpringCloud Gateway）
5. 服务配置（Spring Cloud Config）
6. 服务监控（Spring Cloud Sleuth和SpringBoot Admin）

