如果说断言（Predicate）是为了路由的匹配，那么过滤器则是在请求源服务器之前或者之后对HTTP请求和响应的拦截，以便对请求和响应做出相应的修改，如请求头和响应头的修改。为了做到这点，Gateway提供了对应的过滤器（Filter)



Gateway的过滤器分为全局过滤器和局部过滤器。全局过滤器针对所有路由有效，而局部过滤器则只针对某些路由有效。全局过滤器需要实现GlobalFilter接口，而局部过滤器则要实现GatewayFilter接口

### 内置过滤器工厂

在Gateway中也存在不同的过滤器工厂，并且已经将它们内置。它们提供了多种功能，便于我们进行开发。这些过滤器大体分为请求头、响应头、跳转、参数处理、响应状态、Hystrix熔断和限速器等。过滤器工厂是通过接口GatewayFilterFactory进行定义的，该接口还声明了一个apply方法，该方法返回类型为GatewayFilter

#### AddRequestHeader过滤器工厂

AddRequestHeader是一个添加请求头参数的过滤器工厂，通过它可以增加请求参数

```java
.filters(filter-> filter.addRequestHeader("id","1234")
```

#### AddRequestParameter过滤器工厂

AddRequestParameter过滤器工厂可以新增请求参数

#### AddResponseHeader过滤器工厂

AddResponseHeader过滤器工厂可以增加响应头参数

#### Retry过滤器工厂

Retry过滤器工厂是一种定义重试的过滤器工厂。在Retry过滤器工厂中有以下5个参数

1. retries：重试次数，非负整数。
2. statuses：根据HTTP响应状态来断定是否重试。当请求返回对应的响应码时，进行重试，用枚举org.springframework.http.HttpStatus表示。
3. methods：请求方法，如GET、POST、PUT和DELETE等。使用枚举org.springframework.http. HttpMethod表示。
4. series：重试的状态码系列，取响应码的第一位，按HTTP响应状态码规范取值范围为1 ~ 5，其中，1代表消息，2代表成功，3代表重定向，4代表客户端错误，5代表服务端错误。
5. exceptions：请求异常列表，默认的情况下包含IOException和TimeoutException两种异常。一般来说都不需要我们配置，所以后文不再介绍

#### Hystrix过滤器工厂

Hystrix过滤器工厂提供的是熔断功能，当请求失败或者出现异常的时候，就可以进行熔断了。而一般熔断发生后，会通过降级服务来提高用户体验，所以这往往还会涉及跳转的功能。因此，这里需要先创建请求失败跳转的路径

#### RequestRateLimiter过滤器工厂

RequestRateLimiter工厂用于限制请求流量，避免过大的流量进入系统，从而保证系统在一个安全的流量下可用。在Gateway中提供了RateLimiter<C>接口来定义限流器，该接口中唯一的非抽象实现类是RedisRateLimiter，也就是当前只能通过Redis来实现限流。使用Redis的好处是，可以通过Redis随时监控，但是始终需要通过网络连接第三方服务器，会造成一定的性能消耗，所以我并不推荐这种方式，因此这里只简单介绍限流过滤器工厂，而不深入介绍它的使用。我认为使用Resilience4j限流器可能更好，因为Resilience4j限流器是基于本地内存的，不依赖第三方服务，所以速度更快，性能更好，并且提供Spring Boot度量监控实时情况

### 自定义过滤器

#### 自定义过滤器——使用Resilience4j限流

在Gateway中，过滤器是通过接口GatewayFilter来定义的，所以先来看一下它的源码

##### GatewayFilter源码

```java
public interface GatewayFilter extends ShortcutConfigurable {
    String NAME_KEY = "name";
    String VALUE_KEY = "value";
    Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain);
}
```

从定义来说，十分简单，就一个filter方法而已，只有两个参数，一个是exchange，另一个是chain，它们的含义如下。

exchange：数据交换对象，通过它的getRequest方法可以获取请求对象（ServerHttpRequest），通过它的getResponse方法可以得到响应对象（ServerHttpResponse）。这里的ServerHttpRequest和ServerHttpResponse，与Servlet规范里的HttpServletRequest和HttpServletResponse，很类似，对比来使用就很好理解它们了。

chain：Gateway过滤器责任链，调用它的filter(ServerWebExchange)方法，表示继续执行责任链里后续的过滤器

```yaml
resilience4j:
  ratelimiter:
    limiters:
      # 名称为usr 的限速器
      user:
        # 时间戳内限制通过的请求数
        limitForPeriod: 3
        # 配置时间戳
        limitRefreshPeriod: 10000
        # 超时时间
        timeoutDuration: 5
```

```java
public class LimitFilter implements GatewayFilter {
    private final Logger logger= LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RateLimiter user = rateLimiterRegistry.rateLimiter("user");
        Callable<String> callable = () ->
        {
            logger.info("date [{}]", LocalDateTime.now());
            return "success";
        };
//         绑定限速器
        Callable<String> call = RateLimiter.decorateCallable(user, callable);
//        尝试获取结果
        Try<String> recover = Try.of(call::call).recover(throwable -> {
//           降级逻辑
            logger.info(throwable.getMessage(), throwable);
            return "TOO MANY REQUEST";
        });
        String s = recover.get();
        if ("success".equals(s)) {
            return chain.filter(exchange);
        }
//        超过限流的处理
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        DataBuffer wrap = response.bufferFactory().wrap("请求过多".getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(wrap));
    }
}

    @Bean
    public GatewayFilter limitFilter() {
        return new LimitFilter();
    }

RouteLocatorBuilder.Builder route = builder.routes().route("/user/**",
                r -> r.query("id").filters(filter -> filter.filter(limitFilter())).uri("http://localhost:6001"));
        return route.build();
```

### 全局过滤器

在Gateway的机制中，只要一个类实现了GlobalFilter接口，并且装配到IoC容器中，Gateway就会将它识别为全局过滤器。对于全局过滤器，执行路由命令的处理器（FilteringWebHandler）会把它转变为GatewayFilter，放到过滤器链中的，然后再去执行这条过滤器链

要定义一个全局过滤器，只需要实现GlobalFilter接口，并且装配到IoC容器中即可

```java
public class TokenFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (StringUtils.isNotEmpty(exchange.getRequest().getHeaders().getFirst("token"))) {
            return chain.filter(exchange);
        } else {
            String token = exchange.getRequest().getQueryParams().getFirst("token");
            if (StringUtils.isNotEmpty(token)) {
                ServerHttpRequest request = exchange.getRequest().mutate().header("token", token).build();
                return chain.filter(exchange.mutate().request(request).build());
            } else {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
                DataBuffer wrap = response.bufferFactory().wrap("no token info".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(wrap));
            }
        }
    }
}
	// 注入bean
    @Bean
    public GlobalFilter tokenFilter() {
        return new TokenFilter();
    }
```

### 过滤器排序

Gateway也定义了一个Ordered（org.springframework.core.Ordered）接口来支持排序

这些全局过滤器都实现了GlobalFilter和Ordered接口，所以它们都具备排序的功能。在Ordered接口中有两个常量，一个是HIGHEST_PRECEDENCE（最高优先级），它的取值为最小整数，也就是Integer.MIN_VALUE（-2 147 483 648）；另一个是LOWEST_PRECEDENCE（最低优先级），它的取值为最大整数，也就是Integer.MAX_VALUE（2 147 483 647）

只需要实现Orderd 接口，重写getOrder 方法即可



如果想对局部过滤器进行排序，也可以类似全局过滤器那样实现Ordered接口。此外，在Gateway中，还有一个适配类——OrderedGatewayFilter，通过它也可以实现对过滤器进行排序



