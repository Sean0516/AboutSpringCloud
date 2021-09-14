package com.duplicall.gateway.filter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.Callable;

/**
 * @Description LimitFilter
 * @Author Sean
 * @Date 2021/9/13 18:37
 * @Version 1.0
 */
public class LimitFilter implements GatewayFilter {
    private final Logger logger= LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        RateLimiter user = rateLimiterRegistry.rateLimiter("user");
        Callable<String> callable = () -> {
            logger.info("date [{}]", LocalDateTime.now());
            return "success";
        };
//         绑定限速器
        Callable<String> call = RateLimiter.decorateCallable(user, callable);
//        尝试获取结果
        Try<String> recover = Try.of(call::call).recover(throwable -> {
//           降级逻辑
            logger.info(throwable.getMessage(), throwable);
            return "TOO MANY REQUEST";
        });
        String s = recover.get();
        if ("success".equals(s)) {
            return chain.filter(exchange);
        }
//        超过限流的处理
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        DataBuffer wrap = response.bufferFactory().wrap("请求过多".getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(wrap));
    }
}
