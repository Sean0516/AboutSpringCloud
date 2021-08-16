package com.duplicall.config;

import com.duplicall.config.user.UserConfiguration;
import com.netflix.discovery.endpoint.EndpointUtils;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.RandomRule;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerListFilter;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.ZonePreferenceServerListFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description RibbonGlobalConfiguration
 * @Author Sean
 * @Date 2021/8/16 14:49
 * @Version 1.0
 */
@Configuration
@RibbonClient(name = "USER", configuration = UserConfiguration.class)
public class RibbonGlobalConfiguration {
//    /**
//     * 服务过滤器
//     * @return
//     */
//    @Bean(name = "ribbonServerListFilter")
//    public ServerListFilter<Server> serverListFilter() {
//        // 使用优先选择的过滤器
//        ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
//        filter.setZone(EndpointUtils.DEFAULT_ZONE);
//        return filter;
//    }
//
//    /**
//     * 负载均衡策略
//     * @return
//     */
//    @Bean
//    public IRule rule() {
//        // 使用随机选择服务的策略
//        return new RandomRule();
//    }
}
