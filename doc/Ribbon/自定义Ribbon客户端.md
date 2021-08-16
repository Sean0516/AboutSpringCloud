#### 全局配置

定义一个全局的策略很简单，只需要类似Spring Bean那样处理就可以了。例如，下面我们修改ServerListFilter和负载均衡的策略

```java
@Configuration
public class RibbonGlobalConfiguration {
    // 服务过滤器
    @Bean(name = "ribbonServerListFilter")
    public ServerListFilter<Server> serverListFilter() {
        // 使用优先选择的过滤器
        ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
        filter.setZone(EndpointUtils.DEFAULT_ZONE);
        return filter;
    }
    // 负载均衡策略
    @Bean
    public IRule rule() {
        // 使用随机选择服务的策略
        return new RandomRule();
    }
}
```

#### 局部定义

有时候我们只想对其中的某个微服务使用特殊的策略，为此Ribbon提供了下面这些相关配置项

● <clientName>.ribbon.NFLoadBalancerClassName：负载均衡类，需实现。ILoadBalancer接口。

●<clientName>.ribbon.NFLoadBalancerRuleClassName：负载均衡策略，需实现IRule接口。

●<clientName>.ribbon.NFLoadBalancerPingClassName：心跳监测类，需实现IPing接口。

●<clientName>.ribbon.NIWSServerListClassName：服务实例清单类，需实现ServerList接口。

●<clientName>.ribbon.NIWSServerListFilterClassName：服务实例清单过滤类，需实现ServerListFilter接口

Spring Cloud还提供了@RibbonClient和@RibbonClients。针对单个微服务配置类使用@RibbonClient，针对多个微服务配置使用@RibbonClients

这两个注解的优先级没有代码清单4-27和代码清单4-28生成的对象高，因此在需要使用这两个注解的。我们在配置类GlobalConfiguration上加入注解@RibbonClien

```java
@RibbonClient(name = "USER", configuration = UserConfiguration.class)
```

