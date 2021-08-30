在使用了注解@LoadBalanced后，LoadBalancerClient接口对象就会对RestTemplate进行处理。所以这里我们需要稍微研究一下LoadBalancerClient接口，在Spring Cloud中，它扩展了ServiceInstanceChooser接口，并且存在一个实现类RibbonLoadBalancerClient

#### ServiceInstanceChooser接口定义了一个方法：

```Java
public interface ServiceInstanceChooser {
    ServiceInstance choose(String serviceId);
}
```

这个方法的参数serviceId指代的是微服务的ID，也就是实例的配置项spring.application.name，通过它根据一定的策略能返回一个具体的微服务实例

#### LoadBalancerClient的源码

```java
public interface LoadBalancerClient extends ServiceInstanceChooser {
    <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException; // 根据Service Id 找到具体的服务实例执行请求

    <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException; // 根据服务id 以及服务实例执行请求

    URI reconstructURI(ServiceInstance instance, URI original); //  根据当前给出的URI  重构可用的URL
}
```

#### RibbonLoadBalancerClient 源码

```java
    public <T> T execute(String serviceId, LoadBalancerRequest<T> request, Object hint) throws IOException {
        // 负载均衡器
        ILoadBalancer loadBalancer = this.getLoadBalancer(serviceId);
        // 获取具体服务实例
        Server server = this.getServer(loadBalancer, hint);
        if (server == null) { // 获取结果为空，抛出异常
            throw new IllegalStateException("No instances available for " + serviceId);
        } else {
            // 包装Ribbon 服务实例
            RibbonLoadBalancerClient.RibbonServer ribbonServer = new RibbonLoadBalancerClient.RibbonServer(serviceId, server, this.isSecure(server, serviceId), this.serverIntrospector(serviceId).getMetadata(server));
           	// 调度另外一个execute 方法执行请求
            return this.execute(serviceId, (ServiceInstance)ribbonServer, (LoadBalancerRequest)request);
        }
    }
	// 在服务实例清单中，按照一定的策略选择具体的服务实例
    protected Server getServer(ILoadBalancer loadBalancer, Object hint) {
        return loadBalancer == null ? null : loadBalancer.chooseServer(hint != null ? hint : "default");
    }

public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
        Server server = null;
        if (serviceInstance instanceof RibbonLoadBalancerClient.RibbonServer) {
            server = ((RibbonLoadBalancerClient.RibbonServer)serviceInstance).getServer();
        }

        if (server == null) {
            throw new IllegalStateException("No instances available for " + serviceId);
        } else {
            // 创建分析记录器
            RibbonLoadBalancerContext context = this.clientFactory.getLoadBalancerContext(serviceId);
            RibbonStatsRecorder statsRecorder = new RibbonStatsRecorder(context, server);

            try {
                // 将请求发送到具体的服务实例
                T returnVal = request.apply(serviceInstance);
                // 将结果记录到分析记录器中
                statsRecorder.recordStats(returnVal);
                // 返回请求结果
                return returnVal;
            } catch (IOException var8) {
                statsRecorder.recordStats(var8);
                throw var8;
            } catch (Exception var9) {
                statsRecorder.recordStats(var9);
                ReflectionUtils.rethrowRuntimeException(var9);
                return null;
            }
        }
    }
```

Ribbon中提供了拦截器LoadBalancer Interceptor，对标注@LoadBalanced注解的RestTemplate进行拦截，然后植入LoadBalancerClient的逻辑，下面看一下它的源码

#### LoadBalancerInterceptor的源码

```java
public class LoadBalancerInterceptor implements ClientHttpRequestInterceptor {
    // 负载均衡客户端
    private LoadBalancerClient loadBalancer;
    private LoadBalancerRequestFactory requestFactory;

    public LoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRequestFactory requestFactory) {
        this.loadBalancer = loadBalancer;
        this.requestFactory = requestFactory;
    }

    public LoadBalancerInterceptor(LoadBalancerClient loadBalancer) {
        this(loadBalancer, new LoadBalancerRequestFactory(loadBalancer));
    }
	// 拦截逻辑
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body, final ClientHttpRequestExecution execution) throws IOException {
        URI originalUri = request.getURI();
        String serviceName = originalUri.getHost();
        Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);
        return (ClientHttpResponse)this.loadBalancer.execute(serviceName, this.requestFactory.createRequest(request, body, execution));
    }
}
```

#### LoadBalancerAutoConfiguration的源码

```java
@ConditionalOnClass({RestTemplate.class})
@ConditionalOnBean({LoadBalancerClient.class})
@EnableConfigurationProperties({LoadBalancerRetryProperties.class})
public class LoadBalancerAutoConfiguration {
    @LoadBalanced
    @Autowired(
        required = false
    )
    private List<RestTemplate> restTemplates = Collections.emptyList();
    @Autowired(
        required = false
    )
    private List<LoadBalancerRequestTransformer> transformers = Collections.emptyList();

    @Bean
    public SmartInitializingSingleton loadBalancedRestTemplateInitializerDeprecated(final ObjectProvider<List<RestTemplateCustomizer>> restTemplateCustomizers) {
        return () -> {
            restTemplateCustomizers.ifAvailable((customizers) -> {
                Iterator var2 = this.restTemplates.iterator();

                while(var2.hasNext()) {
                    RestTemplate restTemplate = (RestTemplate)var2.next();
                    Iterator var4 = customizers.iterator();

                    while(var4.hasNext()) {
                        RestTemplateCustomizer customizer = (RestTemplateCustomizer)var4.next();
                        customizer.customize(restTemplate);
                    }
                }

            });
        };
    }

    @ConditionalOnMissingClass({"org.springframework.retry.support.RetryTemplate"})
    static class LoadBalancerInterceptorConfig {
        LoadBalancerInterceptorConfig() {
        }
		//  创建拦截器
        @Bean
        public LoadBalancerInterceptor ribbonInterceptor(LoadBalancerClient loadBalancerClient, LoadBalancerRequestFactory requestFactory) {
            return new LoadBalancerInterceptor(loadBalancerClient, requestFactory);
        }
		// 增加拦截器
        @Bean
        @ConditionalOnMissingBean
        public RestTemplateCustomizer restTemplateCustomizer(final LoadBalancerInterceptor loadBalancerInterceptor) {
            return (restTemplate) -> {
                List<ClientHttpRequestInterceptor> list = new ArrayList(restTemplate.getInterceptors());
                list.add(loadBalancerInterceptor);
                restTemplate.setInterceptors(list);
            };
        }
    }
```

LoadBalancerAutoConfiguration 主要做了以下事情：

1. 创建LoadBalancerInterceptor对象，这样就存在了拦截器，用于拦截相应的被标注了@LoadBalanced的RestTemplate对象
2. 创建RestTemplateCustomizer对象，并且将拦截器设置到已有的拦截列表中，这样LoadBalancerInterceptor对象就可以拦截RestTemplate对象了
3. 维护一个被标注@LoadBalanced的RestTemplate列表，通过RestTemplateCustomizer给需要负载均衡的RestTemplate提供拦截器（LoadBalancerInterceptor）

Ribbon实现负载均衡的流程：

首先通过LoadBalancerInterceptor拦截RestTemplate，然后在其intercept方法调用LoadBalancerClient接口的execute方法来执行负载均衡。