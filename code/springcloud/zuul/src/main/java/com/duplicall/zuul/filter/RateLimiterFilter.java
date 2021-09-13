package com.duplicall.zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.control.Try;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.Callable;

/**
 * @Description RateLimiterFilter
 * @Author Sean
 * @Date 2021/9/13 11:26
 * @Version 1.0
 */
@Component
public class RateLimiterFilter extends ZuulFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    //    注入限速器注册机
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 20;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        String uri = currentContext.getRequest().getRequestURI();
        logger.info("request uri [{}]", uri);
        return uri.startsWith("/u/");
    }

    @Override
    public Object run() throws ZuulException {
        // 获取Resilience 4j 限速器
        RateLimiter user = rateLimiterRegistry.rateLimiter("user");
        Callable<String> callable = () ->
        {
            logger.info("date [{}]", LocalDateTime.now());
            return "success";
        };
//         绑定限速器
        Callable<String> call = RateLimiter.decorateCallable(user, callable);
//        尝试获取结果
        Try<String> recover = Try.of(call::call).recover(throwable -> {
//           降级逻辑
            logger.info(throwable.getMessage(), throwable);
            return "error";
        });
        String s = recover.get();
        if (!"error".equals(s)) {
            return null;
        }
//        超过限流的处理
        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.setResponseStatusCode(HttpStatus.TOO_MANY_REQUESTS.value());
        currentContext.getResponse().setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        currentContext.setResponseBody("超过限流");
        return null;
    }
}
