| 接口定义           | Spring Bean Name        | 默认实现类                     | 说明                                           |
| ------------------ | ----------------------- | ------------------------------ | ---------------------------------------------- |
| IClientConfig      | RibbonClientConfig      | DefaultClientConfigImpl        | 客户端配置，通过它配置Ribbon 相关的内容        |
| IRule              | RibbonRule              | ZoneAvoidanceRule              | 负载均衡策略，具体的负载均衡是通过他来提供算法 |
| IPing              | RibbonRing              | DummyPing                      | 通过ping 命令验证服务实例是否可用              |
| ServerList<Server> | RibbonServerList        | ConfigurationBasedServerList   | 服务实例清单                                   |
| ServerListFilter<> | RibbonServerListFilter  | ZonePreferenceServerListFilter | 根据某些条件过滤后得到的服务实例清单           |
| ILoadBalancer      | RibbonLoadBanlancer     | ZoneAwareLoadBalancer          | 负载均衡器，他将按照某种策略来选取服务器实例   |
| ServerListUpdater  | RibbonServerListUpdater | PollingServerListUpdater       | 根据一定的策略来更新服务实例清单               |

#### Ribbon是如何实现负载均衡的

在使用了注解@LoadBalanced后，LoadBalancerClient接口对象就会对RestTemplate进行处理 

LoadBalancerClient接口在Spring Cloud中，它扩展了ServiceInstanceChooser接口，并且存在一个实现类RibbonLoadBalancerClient 

ServiceInstanceChooser接口定义了一个方法  

```java
ServiceInstance choose(String serviceId);
```

这个方法的参数serviceId指代的是微服务的ID，也就是实例的配置项spring.application.name，通过它根据一定的策略能返回一个具体的微服务实例