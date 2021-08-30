package com.duplicall.product.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description ProductController
 * @Author Sean
 * @Date 2021/8/14 13:48
 * @Version 1.0
 */
@RestController
@RequestMapping("product")
public class ProductController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GetMapping("timeout")
    public String timeOut() {
        long l = (long) (Math.random() * 5000);
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            logger.error("sleep time error as [{}]", e.getMessage(), e);
        }
        return "success";
    }

    @GetMapping("exc/{id}")
    public String exc(@PathVariable(name = "id") String id) {
        if ("000".equals(id)) {
            return "success";
        } else {
            throw new RuntimeException("error as demo test");
        }
    }
}
