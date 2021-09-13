package com.duplicall.zuul.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description HystrixController
 * @Author Sean
 * @Date 2021/9/13 18:27
 * @Version 1.0
 */
@RestController
@RequestMapping("gateway")
public class HystrixController {
    @RequestMapping("hystrix")
    public String hystrix(){
        return "服务降级";
    }
}
