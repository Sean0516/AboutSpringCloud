### Zuul 原理 -- 过滤器

Zuul的原理并非十分复杂，相反的，可能还算比较简单，它的本质就是一套Servlet的API。其中ZuulServlet是核心Servlet，它将接收各类请求。此外NetflixZuul还提供了ZuulServletFilter，它是一个拦截器，可以拦截各类请求。ZuulServlet和ZuulServletFilter就是Zuul的核心内容。为了更加方便地增加和删除拦截逻辑，在ZuulServlet和ZuulServletFilter的基础上，Netflix Zuul定义了自己的过滤器——ZuulFilter。而ZuulFilter就是本章的核心内容，基本网关大部分的逻辑都要通过它来实现

![image-20210913100459373](https://gitee.com/Sean0516/image/raw/master/img/image-20210913100459373.png)

当我们继承ZuulFilter后，需要实现这4个方法，而系统中已经提供了许多ZuulFilter的实现类，它们已经实现了这4个抽象方法。下面我们来了解这4个方法的作用

- shouldFilter：是否执行过滤器逻辑，也就是可以根据上下文判定是否采用过滤器拦截请求。
- run：过滤器的具体逻辑，它是过滤器的核心方法，将返回一个Object对象，倘若返回为null，则表示继续后续的正常逻辑。
- filterType：过滤器类型，有4种类型可设置：“pre”“route”“post”和“error”。
- filterOrder：设置过滤器的顺序

 filterType方法返回的字符串代表的是过滤类型，该类型是以源服务器进行区分的。按其定义分为4种

- pre：在路由到源服务器前执行的逻辑，如鉴权、选择具体的源服务器节点和参数处理等，都可以在这里实现。

- route：执行路由到源服务器的逻辑，如之前我们谈到的Apache HttpClient或者Netflix Ribbon，当前也支持OKHttp。

- post：在路由到源服务器后执行的过滤器，常见的用法是把标准的HTTP响应头添加到响应中，此外也可以通过它来收集响应的度量数据，统计成功率，还可以对源服务器请求返回的数据再次加工，然后返回到客户端，等等。

- error：倘若在整个路由源服务器的执行的过程中发生异常，就可以进入此类过滤器，它可以做全局的响应逻辑处理错误

Zuul会将多个过滤器组织为一个责任链，那么各个过滤器会以什么顺序组织呢？这是由filterOrder方法决定的，它是返回一个数字，该数字越小，在过滤器链中就越优先执行

​															Netflix Zuul过滤器原理图解

![](https://gitee.com/Sean0516/image/raw/master/img/image-20210913100628774.png)

### Netflix Zuul过滤器原理图解

```java
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        try {
            // 初始化RequestContext 对象，它将使用ThreadLocal 保存HTTP 请求和响应信息
            this.init((HttpServletRequest)servletRequest, (HttpServletResponse)servletResponse);
            RequestContext context = RequestContext.getCurrentContext();
            // 将请求设置为一个Zuul 引擎，Zuul 中的各种过滤器通过Zuul 引擎 (ZuulRunner) 允许的
            context.setZuulEngineRan();
			
            try {
                // pre 类型过滤器
                this.preRoute();
            } catch (ZuulException var13) {
                this.error(var13);
                this.postRoute();
                return;
            }

            try {
                //route 过滤器
                this.route();
            } catch (ZuulException var12) {
                this.error(var12);
                this.postRoute();
                return;
            }

            try {
                // post类型过滤器
                this.postRoute();
            } catch (ZuulException var11) {
                this.error(var11);
            }
        } catch (Throwable var14) {
            this.error(new ZuulException(var14, 500, "UNHANDLED_EXCEPTION_" + var14.getClass().getName()));
        } finally {
            RequestContext.getCurrentContext().unset();
        }
    }

```

###  自定义Zuul 过滤器开发

自定义Zuul 过滤器， 主要需要设置过滤类型，以及顺序，和判断那些需要执行过滤器逻辑。 同时，根据实际业务场景写过滤器逻辑

```java
@Component
public class ValidateFilter extends ZuulFilter {
    /**
     * 过滤器类型
     *
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    /**
     * 过滤器顺序
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 15;
    }

    /**
     * 是否需要执行过滤器逻辑
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        Map<String, List<String>> requestQueryParams = currentContext.getRequestQueryParams();

        if (Objects.isNull(requestQueryParams)) {
            return false;
        }
        return requestQueryParams.containsKey("valid");
    }

    /**
     * 具体的过滤器逻辑
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext currentContext = RequestContext.getCurrentContext();
        String valid = currentContext.getRequest().getParameter("valid");
        if ("123456".equals(valid)) {
            return null;
        }
        currentContext.setResponseStatusCode(HttpStatus.SC_UNAUTHORIZED);
        currentContext.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
        currentContext.setResponseBody("no valid code");
        return null;
    }
}
```

网关主要作用有两个，一个是接收和转发请求，而不是业务处理，负载的业务逻辑应该放在具体的资源服务器里面，另一个是保证性能，作为保护源服务器的管卡，应该能快速判定请求的时效性。同时，把一些常用的判定规则数据转载在redis 中，通过过滤器快速判断请求的有效性，就能把很多恶意请求和无效请求过滤掉，从而保护资源服务器。 当然，保护资源服务器的常用方法还有限流算法。 