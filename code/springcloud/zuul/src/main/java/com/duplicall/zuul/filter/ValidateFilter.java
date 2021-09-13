package com.duplicall.zuul.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.apache.http.HttpStatus;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description ValidateFilter
 * @Author Sean
 * @Date 2021/9/13 10:33
 * @Version 1.0
 */
@Component
public class ValidateFilter extends ZuulFilter {
    /**
     * 过滤器类型
     *
     * @return
     */
    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE;
    }

    /**
     * 过滤器顺序
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 15;
    }

    /**
     * 是否需要执行过滤器逻辑
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        Map<String, List<String>> requestQueryParams = currentContext.getRequestQueryParams();

        if (Objects.isNull(requestQueryParams)) {
            return false;
        }
        return requestQueryParams.containsKey("valid");
    }

    /**
     * 具体的过滤器逻辑
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext currentContext = RequestContext.getCurrentContext();
        String valid = currentContext.getRequest().getParameter("valid");
        if ("123456".equals(valid)) {
            return null;
        }
        currentContext.setResponseStatusCode(HttpStatus.SC_UNAUTHORIZED);
        currentContext.getResponse().setContentType(MediaType.APPLICATION_JSON_VALUE);
        currentContext.setResponseBody("no valid code");
        return null;
    }
}
