package com.duplicall.gateway.route;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * @Description RouteConfig
 * @Author Sean
 * @Date 2021/9/13 16:32
 * @Version 1.0
 */
@Configuration
public class RouteConfig {
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
//        ZonedDateTime before = LocalDateTime.now().plusMinutes(2).atZone(ZoneId.systemDefault());
//        return builder.routes().route("/user/**", r -> r.before(before).uri("http://localhost:6001"))
//                .build();
/*        ZonedDateTime after = LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault());
        return builder.routes().route("/user/**", r -> r.after(after).uri("http://localhost:6001"))
                .build(); */
        ZonedDateTime start = LocalDateTime.now().plusSeconds(10).atZone(ZoneId.systemDefault());
        ZonedDateTime end = LocalDateTime.now().plusMinutes(4).atZone(ZoneId.systemDefault());
        RouteLocatorBuilder.Builder route = builder.routes().route("/user/**", r -> r.query("id").filters(filter -> filter.hystrix(config -> {
            config.setName("hystrix");
//            config.setFallbackUri("forward:/gateway/hystrix");
        })).uri("http://localhost:6001"));

        return route.build();


    }
}
