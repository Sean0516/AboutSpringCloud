package com.duplicall.user.services.user;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @Description UserImpl
 * @Author Sean
 * @Date 2021/8/23 9:15
 * @Version 1.0
 */
@Service
public class UserImpl implements IUser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RestTemplate restTemplate;

    @Override
    @HystrixCommand(fallbackMethod = "fallback1")
    public String timeOut() {
        String url = "http://PRODUCT/product/timeout";
        String body = restTemplate.getForEntity(url, String.class).getBody();
        logger.info("return msg [{}] ", body);
        return "success ";
    }

    @Override
    @HystrixCommand(fallbackMethod = "fallback2")
    public String exception(String id) {
        String url = "http://PRODUCT/product/exc/" + id;
        String body = restTemplate.getForEntity(url, String.class).getBody();
        logger.info("exc return msg [{}] ", body);
        return "success ";
    }
    public String fallback1(){
        return "请求超时了";
    }
    public String fallback2(String id){
        return "请求产生了异常" + id;
    }
}
