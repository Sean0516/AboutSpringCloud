网关的一个重要应用就是限制流量。虽然我们可以通过Zuul的过滤器来验证有效请求和无效请求，然后把无效请求隔离在服务之外。但是有时候有效请求的量会很大，远远超过服务可承受的范围，这个时候可以依靠限流算法，限制单位时间流入的请求数，保证服务可用，避免出现雪崩效应

利用我们学习过的新一代Spring Cloud选择的Resilience4j限速器（RateLimiter）进行限速，也可以使用外国开发者提供的spring-cloud-zuul-ratelimit包。不过在此之前，需要加入依赖

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

配置好之后，需要重写ZullFilter 让限速器生效

```java
@Component
public class RateLimiterFilter extends ZuulFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    //    注入限速器注册机
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 20;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        String uri = currentContext.getRequest().getRequestURI();
        logger.info("request uri [{}]", uri);
        return uri.startsWith("/u/");
    }

    @Override
    public Object run() throws ZuulException {
        // 获取Resilience 4j 限速器
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
            return "error";
        });
        String s = recover.get();
        if (!"error".equals(s)) {
            return null;
        }
//        超过限流的处理
        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.setResponseStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
        currentContext.getResponse().setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        currentContext.setResponseBody("超过限流");
        return null;
    }
}
```

