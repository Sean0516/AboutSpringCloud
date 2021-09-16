package com.duplicall.config_user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class ConfigUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigUserApplication.class, args);
    }

    @Value("${version}")
    public String version;

    @RequestMapping("version")
    public String version() {
        return version;
    }
}
