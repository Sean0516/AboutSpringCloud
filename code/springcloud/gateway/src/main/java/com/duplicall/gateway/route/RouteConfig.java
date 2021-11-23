package com.duplicall.gateway.route;

import com.duplicall.gateway.filter.LimitFilter;
import com.duplicall.gateway.filter.TokenFilter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Callable;

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

        RouteLocatorBuilder.Builder route = builder.routes().route("user-service",
                r -> r.path("/**/**").and().uri("lb://user"));
        return route.build();
    }

//    @Bean
//    public GlobalFilter tokenFilter() {
//        return new TokenFilter();
//    }
//
//    @Bean
//    public GatewayFilter limitFilter() {
//        return new LimitFilter();
//    }
}
