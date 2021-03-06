package com.duplicall.config_client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ConfigClientApplication {
    @Value("${version}")
    public String version;
    public static void main(String[] args) {
        SpringApplication.run(ConfigClientApplication.class, args);
    }
    @RequestMapping("version")
    public String version(){
        return version;
    }
}
