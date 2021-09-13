### GateWay 的执行方式

1. 创建一条线程，通过类似Zuul的过滤器拦截请求
2. 对资源服务器转发请求，但注意，Gateway 并不会等待请求调用服务器器的过程，而是将处理线程挂起，这样便不再占用资源了
3. 等资源服务器返回消息后，再通过寻址的方式来响应之前客户端发送的请求



这里需要注意的有两点。

1. 因为Gateway依赖WebFlux，而WebFlux和Spring Web MVC的包冲突，所以项目再引入spring- boot-starter-web就会发生异常。
2. 其次，当前Gateway只能支持Netty容器，不支持其他容器，所以引入Tomcat或者Jetty等容器就会在运行期间出现意想不到的问题

以在pom.xml中应该删除对spring-boot-starter-web和其他容器的依赖包。如果你创建模块时不小心选择了WAR打包方式，那么还需要删除IDE为你创建的ServletInitializer.java文件，这是一个被扫描的类，是依赖Servlet容器的，而这里使用的是Netty容器，没有Servlet容器，所以它的存在会引发错误。

### Gateway 的执行原理

#### 路由（route）

路由网关是一个最基本的组件，它由ID、目标URI、断言集合和过滤器集合共同组成，当断言判定为true时，才会匹配到路由

#### 断言（predicate）

它主要是判定当前请求如何匹配路由，采用的是Java 8断言。可以存在多个断言，每个断言的入参都是Spring框架的ServerWebExchange对象类型。它允许开发者匹配来自HTTP请求的任何内容，例如URL、请求头或请求参数，当这些断言都返回true时，才执行这个路由

#### 过滤器（filter）

这些是使用特定工厂构造的SpringFrameworkGatewayFilter实例，作用是在发送下游请求之前或者之后，修改请求和响应。和断言一样，过滤器也可以有多个





