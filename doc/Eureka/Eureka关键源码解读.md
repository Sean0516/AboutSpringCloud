在Eureka的机制中，主要是客户端主动维护和Eureka服务端的关系，所以这里的源码都是Eureka客户端的逻辑代码

##### 类EndpointUtils的getServiceUrlsFromConfig方法的源码

```java
public static List<String> getServiceUrlsFromConfig(EurekaClientConfig clientConfig, String instanceZone, boolean preferSameZone) {
        List<String> orderedUrls = new ArrayList();
    	// 寻找 region
        String region = getRegion(clientConfig);
    // 寻找可用zone
        String[] availZones = clientConfig.getAvailabilityZones(clientConfig.getRegion());
        if (availZones == null || availZones.length == 0) {
            availZones = new String[]{"default"};
        }

        logger.debug("The availability zone for the given region {} are {}", region, availZones);
    	// 从可用zone  数组中检索出当前实例的zone 下班，如果找不到则返回0，从而指向默认zone
        int myZoneOffset = getZoneOffset(instanceZone, preferSameZone, availZones);
     // 根据zone 获取已经存在的service url 
        List<String> serviceUrls = clientConfig.getEurekaServerServiceUrls(availZones[myZoneOffset]);
      // 倘若当前已经存在了对应的service url  ，则加入有序数组
        if (serviceUrls != null) {
            orderedUrls.addAll(serviceUrls);
        }
        int currentOffset = myZoneOffset == availZones.length - 1 ? 0 : myZoneOffset + 1;

        while(currentOffset != myZoneOffset) {
           // 根据zone 从当前配置中读取server  urls
            serviceUrls = clientConfig.getEurekaServerServiceUrls(availZones[currentOffset]);
          
            if (serviceUrls != null) {
                orderedUrls.addAll(serviceUrls);
            }
			// 如果达到数组最后，则从0开始循环
            if (currentOffset == availZones.length - 1) {
                currentOffset = 0;
            } else {
                // 下标递增
                ++currentOffset;
            }
        }
		// 如果都为空，则抛出异常
        if (orderedUrls.size() < 1) {
            throw new IllegalArgumentException("DiscoveryClient: invalid serviceUrl specified!");
        } else {
            return orderedUrls;
        }
    }
```

1. 获取Region，如果没有配置，则使用默认值。一个微服务只能找到一个Region，如果没有找到，就使用默认值
2. 通过Region获取可用的Zone数组，一个Region可以对应多个Zone，如果获取Zone失败，则使用默认值
3. 在可用的Zone数组中查找当前的Zone实例。如果找到第一个匹配Zone的下标，则返回Zone的下标；如果没有找到，则返回0指向默认值
4. 将与Zone匹配的已经配置好的可用serviceUrls加入到orderedUrls中。
5. 遍历可用Zone数组，找到各个Zone匹配的serviceUrls加入到orderedUrls中，最后返回

##### EurekaClientConfig

这里还有一个重要的接口，EurekaClientConfig，它的作用是对Eureka客户端进行配置。接口EurekaClientConfig有两个实现，一个是Netflix公司的DefaultEurekaClientConfig，另一个是Spring Cloud自己开发的EurekaClientConfigBean。我们在配置文件（如application.yml）中以“eureka.client”为前缀的配置项就是配置它的属性。在上述代码中，用到了EurekaClientConfigBean的getEurekaServerServiceUrls方法来获取serviceUrl，为此让我们讨论一下它的源码

```java
// serviceURl 存放处， key 是zone 值是URL
private Map<String, String> serviceUrl = new HashMap();
// 默认的URL
public static final String DEFAULT_URL = "http://localhost:8761/eureka/";
// 默认的Zone
public static final String DEFAULT_ZONE = "defaultZone";

public List<String> getEurekaServerServiceUrls(String myZone) {
        String serviceUrls = (String)this.serviceUrl.get(myZone);
        if (serviceUrls == null || serviceUrls.isEmpty()) {
            serviceUrls = (String)this.serviceUrl.get("defaultZone");
        }

        if (!StringUtils.isEmpty(serviceUrls)) {
            // 多注册的service url  使用半角逗号分隔为数组
            String[] serviceUrlsSplit = StringUtils.commaDelimitedListToStringArray(serviceUrls);
            List<String> eurekaServiceUrls = new ArrayList(serviceUrlsSplit.length);
            String[] var5 = serviceUrlsSplit;
            int var6 = serviceUrlsSplit.length;

            for(int var7 = 0; var7 < var6; ++var7) {
                String eurekaServiceUrl = var5[var7];
                if (!this.endsWithSlash(eurekaServiceUrl)) {
                    eurekaServiceUrl = eurekaServiceUrl + "/";
                }

                eurekaServiceUrls.add(eurekaServiceUrl.trim());
            }

            return eurekaServiceUrls;
        } else {
            return new ArrayList();
        }
    }
```

回到类DiscoveryClient，在其构造方法中，它会调用一个私有的（private）initScheduledTasks方法，从方法名来看，它是一个初始化任务计划的方法。当我们打开它的源码时，会发现它实际分为两个服务，一个是服务获取，另一个是关于服务注册和续约的逻辑。这里先来看一下服务获取的代码

```java
 private void initScheduledTasks() {
        int renewalIntervalInSecs;
        int expBackOffBound;
     // 是否允许服务获取，由 fetch-registry 配置文件控制，默认为true
        if (this.clientConfig.shouldFetchRegistry()) {
            // 获取服务获取注册信息的刷新时间间隔
            renewalIntervalInSecs = this.clientConfig.getRegistryFetchIntervalSeconds();
            // 获取超时最大尝试数，默认为10 次
            expBackOffBound = this.clientConfig.getCacheRefreshExecutorExponentialBackOffBound();
            this.cacheRefreshTask = new TimedSupervisorTask("cacheRefresh", this.scheduler, this.cacheRefreshExecutor, 	                     renewalIntervalInSecs, TimeUnit.SECONDS, expBackOffBound, new DiscoveryClient.CacheRefreshThread());
            // 启动线程按照一定的时间间隔执行服务获取
            this.scheduler.schedule(this.cacheRefreshTask, (long)renewalIntervalInSecs, TimeUnit.SECONDS);
        }

    }

```

服务获取是Eureka客户端的功能，它会通过REST请求从Eureka服务中获取其他Eureka客户端的信息，形成服务实例清单，缓存到本地。在执行服务调用时，就从服务实例清单中获取可用的实例进行调用

##### 服务注册和续约的源码

```java
if (this.clientConfig.shouldRegisterWithEureka()) {
    		// 续约时间 默认为30 秒
            renewalIntervalInSecs = this.instanceInfo.getLeaseInfo().getRenewalIntervalInSecs();
    		// 续约超时后，尝试最大次数，默认为10 
            expBackOffBound = this.clientConfig.getHeartbeatExecutorExponentialBackOffBound();
            logger.info("Starting heartbeat executor: renew interval is: {}", renewalIntervalInSecs);
    		// 心跳服务维持续约
            this.heartbeatTask = new TimedSupervisorTask("heartbeat", this.scheduler, this.heartbeatExecutor, renewalIntervalInSecs, TimeUnit.SECONDS, expBackOffBound, new DiscoveryClient.HeartbeatThread());
            this.scheduler.schedule(this.heartbeatTask, (long)renewalIntervalInSecs, TimeUnit.SECONDS);
    		// 注册线程
            this.instanceInfoReplicator = new InstanceInfoReplicator(this, this.instanceInfo, this.clientConfig.getInstanceInfoReplicationIntervalSeconds(), 2);
    		// 客户端状态监听，如果发生改变，守护线程会相应的维护
            this.statusChangeListener = new StatusChangeListener() {
                public String getId() {
                    return "statusChangeListener";
                }

                public void notify(StatusChangeEvent statusChangeEvent) {
                    if (InstanceStatus.DOWN != statusChangeEvent.getStatus() && InstanceStatus.DOWN != statusChangeEvent.getPreviousStatus()) {
                        DiscoveryClient.logger.info("Saw local status change event {}", statusChangeEvent);
                    } else {
                        DiscoveryClient.logger.warn("Saw local status change event {}", statusChangeEvent);
                    }

                    DiscoveryClient.this.instanceInfoReplicator.onDemandUpdate();
                }
            };
    		// 是否使用后端守护线程监控和更新客户端状态
            if (this.clientConfig.shouldOnDemandUpdateStatusChange()) {
                this.applicationInfoManager.registerStatusChangeListener(this.statusChangeListener);
            }
			// 启动注册线程
            this.instanceInfoReplicator.start(this.clientConfig.getInitialInstanceInfoReplicationIntervalSeconds());
        } else {
            logger.info("Not registering with Eureka server per configuration");
        }
```

这里可以看到服务续约和服务注册也是放在一个代码段中的，它首先会通过配置项（eureka.client. register-with-eureka）判断是否启用注册功能，然后才开始服务续约和注册的功能代码。服务续约和服务获取一样也有两个参数，一个是时间间隔，默认值也是30秒，另一个是最大超时尝试次数，默认值为10。同样，它也是使用定时任务和心跳机制来执行服务续约，避免被Eureka服务器剔除出去的。对于服务注册，则是使用线程类InstanceInfoReplicator实现的。在初识化它的时候，可以看到一个时间间隔参数，这便是注册时间间隔，为什么会有注册时间间隔呢？这是因为Eureka服务器也可能会因为某些原因不可用而需要重新启动，这时，有时间间隔注册的功能，就可以保证Eureka客户端能够自我恢复注册到重新启动的Eureka服务中心中

跟着是状态监听器，也就是说，当状态发生变化的时候，就会通知守护线程来做出对应的动作，以适应Eureka客户端状态变化的场景，不过，是否启用这个功能还需要配置一个是否启动守护线程监听状态的参数（eureka.client.on-demand-update-status-change）。它的默认值为true，所以在默认的情况下，是会使用守护线程去监听状态的。

最后是启动注册线程，调用了start方法，该方法中有一个参数，这个参数是注册线程的初始化延迟时间间隔（eureka.client.initial-instance-info-replication-interval-seconds），它的默认值为40，也就是在Eureka客户端启动的时候，会延迟40秒后才发起注册请求给Eureka服务

##### 服务注册  DiscoveryClient的register方法

```java
boolean register() throws Throwable {
        logger.info("DiscoveryClient_{}: registering service...", this.appPathIdentifier);

        EurekaHttpResponse httpResponse;
        try {
            // 服务注册
            httpResponse = this.eurekaTransport.registrationClient.register(this.instanceInfo);
        } catch (Exception var3) {
            logger.warn("DiscoveryClient_{} - registration failed {}", new Object[]{this.appPathIdentifier, var3.getMessage(), var3});
            throw var3;
        }

        if (logger.isInfoEnabled()) {
            logger.info("DiscoveryClient_{} - registration status: {}", this.appPathIdentifier, httpResponse.getStatusCode());
        }
		// 返回监听值.如果返回值为204 则表示成功，无需返回内容
        return httpResponse.getStatusCode() == Status.NO_CONTENT.getStatusCode();
    }
```

##### RestTemplateTransportClientFactory的register方法

```java
    public EurekaHttpResponse<Void> register(InstanceInfo info) {
        //通过 serviceUrl 构建URL 请求
        String urlPath = this.serviceUrl + "apps/" + info.getAppName();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept-Encoding", "gzip");
        headers.add("Content-Type", "application/json");
        //  REST 风格的POST 请求注册
        ResponseEntity<Void> response = this.restTemplate.exchange(urlPath, HttpMethod.POST, new HttpEntity(info, headers), Void.class, new Object[0]);
        return EurekaHttpResponse.anEurekaHttpResponse(response.getStatusCodeValue()).headers(headersOf(response)).build();
    }
```

