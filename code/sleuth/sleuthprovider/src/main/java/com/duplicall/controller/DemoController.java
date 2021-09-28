package com.duplicall.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description DemoController
 * @Author Sean
 * @Date 2021/9/26 10:52
 * @Version 1.0
 */
@RestController
@RequestMapping("user")
public class DemoController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping("name")
    public String sayHello(String name) {
        logger.info("请求参数 [{}]", name);
        return "Hello " + name;
    }
}
