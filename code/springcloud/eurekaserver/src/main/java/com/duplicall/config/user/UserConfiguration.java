package com.duplicall.config.user;

import com.netflix.loadbalancer.BestAvailableRule;
import com.netflix.loadbalancer.IPing;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.PingUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description UserConfiguation
 * @Author Sean
 * @Date 2021/8/16 15:03
 * @Version 1.0
 */
@Configuration
public class UserConfiguration {
    @Bean
    public IRule rule() {
        return new BestAvailableRule();
    }

    @Bean
    public IPing ping() {
        return new PingUrl();
    }
}
