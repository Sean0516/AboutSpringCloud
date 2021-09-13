### Before 路由断言工厂

Before路由断言是一个关于时间的断言，也就是可以判断路由在什么时间之前有效，过了这个时间点则无效。类似这样的时间断言还有After路由断言工厂和Between路由断言工厂。这些断言工厂都采用UTC时间制度，有时候我们可以在YAML文件中进行配

```java
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        ZonedDateTime zonedDateTime = LocalDateTime.now().plusMinutes(5).atZone(ZoneId.systemDefault());
        return builder.routes().route("/user/**", r -> r.before(zonedDateTime).uri("http://localhost:6001"))
                .build();
    }
```

### After  路由断言工厂

和Before路由断言工厂一样，After路由断言工厂也是一个时间断言，只是它是判断路由在什么时间点之后才有效

```java
     ZonedDateTime after = LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault());
        return builder.routes().route("/user/**", r -> r.before(after).uri("http://localhost:6001"))
                .build();
```

### Between 路由断言工厂

Between路由断言工厂也是一个基于时间的断言，从Between的英文翻译“在……之间”，可以知道，它判断时间是否在两个时间点之间

```java
        ZonedDateTime start = LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault());
        ZonedDateTime end = LocalDateTime.now().plusMinutes(2).atZone(ZoneId.systemDefault());
        return builder.routes().route("/user/**", r -> r.between(start,end).uri("http://localhost:6001"))
                .build();	
```

### Cookie 路由断言工厂

Cookie路由断言工厂是针对Cookie参数判断的，在现实中使用较少，因为从现实的角度来说，用户可以关闭Cookie。Cookie路由断言工厂可以判定某个Cookie参数是否满足某个正则式，当满足时才去匹配路由

在PredicateSpec.cookie方法中，存在两个参数，第一个参数是执行Cookie的参数名，第二个参数是一个正则式。只有当Cookie的参数和正则式匹配时，路由才能成立，否则就不成立

### Header 路由断言工厂

Header路由断言工厂的作用是判定某个请求头参数是否匹配一个正则式。当满足时，路由才会成立，否则就不成立

```java
        RouteLocatorBuilder.Builder route = builder.routes().route("/user/**", r -> r.between(start, end).uri("http://localhost:6001")).route("/user/**", r -> r.header("id","^[0-9]*$" ).uri("http://localhost:6001"));

```

### Host路由断言工厂

一个正常的网址往往需要提供主机（host）名称或者地址才能进行访问，而Host断言是一种限制主机名称的断言。这需要修改hosts文件

#### Method路由断言工厂

Method路由断言工厂用来判断HTTP的请求类型，如判断GET、POST、PUT等请求

### Path路由断言工厂

Path路由断言工厂是通过URI路径判断是否匹配路由的

### Query路由断言工厂

Query路由断言工厂是对请求参数的判定，它分为两类，一类是判断是否存在某些请求参数，另一类是对请求参数值进行验证

### RemoteAddr路由断言工厂

RemoteAddr路由断言工厂，从名字来看，翻译成中文就是“远程服务器地址”，因此它是一个判定服务器地址的断言工厂

### Weight路由断言工厂

Weight路由断言工厂是一种按照权重路由的工厂