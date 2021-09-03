### 使用服务降级

断路器中的服务降级是一个十分重要的概念，那么我们应该如何在feign 配置 他呢

fallback和fallbackFactory，便是OpenFeign为此给我们提供的配置项。不过在此以前，我们需要一个UserFacade接口的实现类

```java
/**
 * 需要类提供服务降级方法，需要满足三个条件
 * 1. 实现openFeign  接口定义的方法
 * 2. 将Bean 注册为spring bean
 * 3. 使用 @FeignClient 的 fallback 配置项指向当前类
 */
@Component
public class UserFallBack implements UserFacade {
    @Override
    public UserInfo getUser(String id) {
        return UserInfo.builder().name(null).age(0).id(null).build();
    }

    @Override
    public String updateUser(UserInfo userInfo) {
        return "服务降级";
    }

    @Override
    public List<UserInfo> userList(String[] ids) {
        return new ArrayList<>();
    }

    @Override
    public String delete(String id) {
        return "服务降级";
    }

    @Override
    public String upload(MultipartFile file) {
        return "服务降级";
    }
}
```

```java
@Component
public class UserFallBackFactory implements FallbackFactory<UserFacade> {
    @Override
    public UserFacade create(Throwable throwable) {
        return new UserFacade() {
            @Override
            public UserInfo getUser(String id) {
                return UserInfo.builder().name("demo").build();
            }

            @Override
            public String updateUser(UserInfo userInfo) {
                return throwable.getMessage();
            }

            @Override
            public List<UserInfo> userList(String[] ids) {
                return null;
            }

            @Override
            public String delete(String id) {
                return throwable.getMessage();
            }

            @Override
            public String upload(MultipartFile file) {
                return throwable.getMessage();
            }
        };
    }
}
```

```java
首先配置Hystrix 为true 
feign:
  hystrix:
    enabled: true
 自定义 fallBack 类 ，有两种方法， 第一种，实现对应UserFacade 第二种实现FallbackFactory的create 方法 ，第二种可以捕获到具体的异常信息
 第三步，指定对应的fallBackFactory       或者 fallBack 
@FeignClient(value = "user", fallbackFactory = UserFallBackFactory.class)


```