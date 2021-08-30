![image-20210823102055163](https://gitee.com/Sean0516/image/raw/master/img/image-20210823102055163.png)

​																										Hystrix工作流程

### Hystrix 命令

HystrixCommand和HystrixObservableCommand，通过它们就可以封装一个Hystrix命令了。其中HystrixCommand是同步请求命令，HystrixObservableCommand是异步请求命令，它们俩使用的都是流的概念，它们的底层实现是RxJava

这里Hystrix会把服务消费者的请求封装成一个HystrixCommand对象或者一个HystrixObservable Command对象，从而使你可以用不同的请求对客户进行参数化，这便是一种命令模式，能达到对“行为请求者”和“行为实现者”解耦的目的。

下面是行Hystrix命令的4种方式

1. execute()：该方法是阻塞的，从依赖请求中接收单个响应（或者出错时抛出异常）
2. queue()：从依赖请求中返回一个包含单个响应的Future对象。
3. observe()：订阅一个从依赖请求中返回的代表响应的Observable对象
4. toObservable()：返回一个Observable对象，只有当你订阅它时，它才会执行Hystrix命令并发射响应。

### 断路器

当命令查询没有缓存的时候，依据图5-9，流程会到达断路器。应该说断路器是Hystrix的核心内容，首先需要清楚的是，在Hystrix中，断路器有3种状态

- CLOSED：关闭
- OPEN：打开。
- HALF_OPEN：半打开

这3种状态存在下面3种可能性

- 倘若断路器状态为OPEN，那么它就会进入第⑧步，直接转到降级方法（fallback）中去
- 倘若断路器状态为CLOSE，那么它就会到第⑤步，继续执行相关的正常逻辑
- 倘若断路器状态为HALF_OPEN，那么它就会再次尝试请求，具体情况后文会再讨论

开始的时候，断路器的状态为CLOSED，也就是关闭状态，这时候我们可以很通畅地执行服务调用。但是，在一定的情况下，断路器的状态会发生变化

- CLOSED状态转换为OPEN状态：但是观察者（Observable）在观察满足一定的条件后，就会通过subscribeToStream方法获取统计分析的数据，用来判断是否转变状态。例如，当发生错误的请求占比达到50%时，就会将断路器状态从CLOSED转变为OPEN
- OPEN和HALF_OPEN的状态转换：之前我们谈到状态转变为了OPEN，此时就会阻隔请求，但是我们也要考虑恢复的问题，毕竟有时候是负荷太大才导致断路的，但是过段时间负荷可能就没有那么大了，就应该考虑恢复了。所以在当断路器打开超过一定时间（默认为5秒）的情况下，它就会进入HALF_OPEN状态，此时可以进行尝试请求，调用attemptExecution方法。但是此调用可能成功，也可能失败，如果成功，则重新将断路器设置为CLOSED状态，放行其他请求；如果不成功，则使用markNonSuccess方法，让断路器的状态继续为OPEN，阻断请求

#### 在Hystrix中断路器需要实现HystrixCircuitBreaker接口，它的大致源码如代码如下

```java
public interface HystrixCircuitBreaker {
    //  判断断路器是否允许发送请求
    boolean allowRequest();
	// 判断断路器是否已经打开
    boolean isOpen();
	// 当执行成功时，记录结果，可能重新关闭断路器
    void markSuccess();
	// 提供空实现
    public static class NoOpCircuitBreaker implements HystrixCircuitBreaker {
        public NoOpCircuitBreaker() {
        }

        public boolean allowRequest() {
            return true;
        }

        public boolean isOpen() {
            return false;
        }

        public void markSuccess() {
        }
    }

   
	// 使用工厂生成断路器
    public static class Factory {
      
    }
}
```

#### 默认断路器实现类 HystrixCircuitBreakerImpl

```java
    public static class HystrixCircuitBreakerImpl implements HystrixCircuitBreaker {
        private final HystrixCommandProperties properties; //属性值
        private final HystrixCommandMetrics metrics; // 度量
        // 断路器开启标志位
        private AtomicBoolean circuitOpen = new AtomicBoolean(false);
        private AtomicLong circuitOpenedOrLastTestedTime = new AtomicLong();

        protected HystrixCircuitBreakerImpl(HystrixCommandKey key, HystrixCommandGroupKey commandGroup, HystrixCommandProperties properties, HystrixCommandMetrics metrics) {
            this.properties = properties;
            this.metrics = metrics;
        }

        public void markSuccess() {
            if (this.circuitOpen.get() && this.circuitOpen.compareAndSet(true, false)) {
                this.metrics.resetStream();
            }

        }

        public boolean allowRequest() {
            if ((Boolean)this.properties.circuitBreakerForceOpen().get()) {
                return false;
            } else if ((Boolean)this.properties.circuitBreakerForceClosed().get()) {
                this.isOpen();
                return true;
            } else {
                return !this.isOpen() || this.allowSingleTest();
            }
        }

        public boolean allowSingleTest() {
            long timeCircuitOpenedOrWasLastTested = this.circuitOpenedOrLastTestedTime.get();
            return this.circuitOpen.get() && System.currentTimeMillis() > timeCircuitOpenedOrWasLastTested + (long)(Integer)this.properties.circuitBreakerSleepWindowInMilliseconds().get() && this.circuitOpenedOrLastTestedTime.compareAndSet(timeCircuitOpenedOrWasLastTested, System.currentTimeMillis());
        }

        public boolean isOpen() {
            if (this.circuitOpen.get()) {
                return true;
            } else {
                HealthCounts health = this.metrics.getHealthCounts();
                if (health.getTotalRequests() < (long)(Integer)this.properties.circuitBreakerRequestVolumeThreshold().get()) {
                    return false;
                } else if (health.getErrorPercentage() < (Integer)this.properties.circuitBreakerErrorThresholdPercentage().get()) {
                    return false;
                } else if (this.circuitOpen.compareAndSet(false, true)) {
                    this.circuitOpenedOrLastTestedTime.set(System.currentTimeMillis());
                    return true;
                } else {
                    return true;
                }
            }
        }
    }

```

#### 舱壁模式

舱壁模式的调用，将服务调用隔离到了各自的线程池内，它们的调用命令都是在各自的线程池内进行的了。虽然产品服务调用增多时，可能会出现大量的线程阻塞，导致其自身服务调用卡顿，甚至抛弃请求，但是它影响的将只是线程池1，而不会影响到线程池2。这样，资金服务和交易服务的调用就仍能保持畅通，不会出现之前所说的，影响到用户服务和资金服务之间的相互协作问题了

Hystrix采用这样的隔离，给应用带来了很多好处

- 依赖服务的调用得到了完全的保护，执行某个依赖服务接口调用时，即使线程占满了对应的线程池，也不会影响其他依赖服务在别的线程池上的调用
- 有效地降低了接入的风险，毕竟许多新的依赖服务接入后，往往存在不稳定的问题或者其他问题。基于这样的隔离，就可以把新接入的服务隔离到单独的线程池中，这样便不会影响现有的依赖服务调用
- 当新的依赖服务从故障变为稳定时，系统只需要恢复一个独立的线程池，而无须做全局维护。这样就能更快地回收资源或者恢复，代价也更小
- 如果是客户端的配置错误，那么线程池可以很快感知错误，并给出提示（反馈调用错误比例、延迟、超时和拒绝次数等），我们就可以在不影响其他服务的情况下，通过动态参数配置来修改，使得服务能动态服务
- 如果是客户端的配置错误，那么线程池可以很快感知错误，并给出提示（反馈调用错误比例、延迟、超时和拒绝次数等），我们就可以在不影响其他服务的情况下，通过动态参数配置来修改，使得服务能动态服务。
- 当服务依赖出现故障或者性能变差的时候，线程池会反馈一些指标（如失败次数、延迟、超时和拒绝等），在隔离之后，我们可以只针对某些线程池进行调整，而无须对整个应用进行维护
- 如果你使用专有的线程池，还可以在同步的基础上构建出异步执行的门面

总之，使用了舱壁模式后，我们能得到很多好处，程序也能变得更健壮，不会造成因为某个依赖服务调度压力变大而使其他调用难以进行的情况，而且我们还可以只针对某个线程池进行维护

### 请求合并

请求合并是Hystrix中除了请求缓存之外的另外一个提高性能的利器。我们之前谈过，通过HTTP协议进行REST调用，实际是一种比较消耗资源的方式。在Hystrix的调用中，它如果正常调用，最终就会通过舱壁模式进入到一个单独的线程池里。当出现高并发场景的时候，这些请求会充满线程池，导致大量的线程挂起，最终导致排队、延迟响应或者超时等现象。为了解决这些问题，Hystrix提供了请求合并的功能，也就是说，在一个很短的时间戳内，按照一定的规则进行判断，如果觉得是同样的请求，就将其合并为同一个请求，只用一条线程进行请求，然后响应多个请求。请注意，这里请求合并的作用域可以是全局性有效的（GLOBAL），也可以是单次请求有效（REQUEST）的，当然，默认情况是单次请求有效。Hystrix中提供的合并请求类是HystrixCollapser，它是一个抽象类

##### HystrixCollapser的源码

```java

package com.netflix.hystrix;

public abstract class HystrixCollapser<BatchReturnType, ResponseType, RequestArgumentType> implements HystrixExecutable<ResponseType>, HystrixObservable<ResponseType> {
	// 获取单次请求参数
    public abstract RequestArgumentType getRequestArgument();
	// 生成合并请求后的 Hystrix 命令
    protected abstract HystrixCommand<BatchReturnType> createCommand(Collection<HystrixCollapser.CollapsedRequest<ResponseType, RequestArgumentType>> var1);
	// 将请求结果分配到各个单次请求中
    protected abstract void mapResponseToRequests(BatchReturnType var1, Collection<HystrixCollapser.CollapsedRequest<ResponseType, RequestArgumentType>> var2);
  
}

```

```java
public @interface HystrixCollapser {
    // 合并器键
    String collapserKey() default "";
	// 合并方法
    String batchMethod();
	// 合并作用域 。 默认请求访问
    Scope scope() default Scope.REQUEST;
	// 合并器属性
    HystrixProperty[] collapserProperties() default {};
}
```

