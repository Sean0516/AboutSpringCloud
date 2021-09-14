package com.duplicall.gateway.filter;

import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @Description TokenFilter
 * @Author Sean
 * @Date 2021/9/14 14:01
 * @Version 1.0
 */
public class TokenFilter implements GlobalFilter , Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (StringUtils.isNotEmpty(exchange.getRequest().getHeaders().getFirst("token"))) {
            return chain.filter(exchange);
        } else {
            String token = exchange.getRequest().getQueryParams().getFirst("token");
            if (StringUtils.isNotEmpty(token)) {
                ServerHttpRequest request = exchange.getRequest().mutate().header("token", token).build();
                return chain.filter(exchange.mutate().request(request).build());
            } else {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
                DataBuffer wrap = response.bufferFactory().wrap("no token info".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(wrap));
            }
        }
    }

    @Override
    public int getOrder() {
        return 10000;
    }
}
