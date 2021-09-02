package com.duplicall.product.controller;

import com.common.UserInfo;
import com.duplicall.product.facade.UserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description FeignController
 * @Author Sean
 * @Date 2021/9/2 10:01
 * @Version 1.0
 */
@RestController
@RequestMapping("feign")
public class FeignController {
    @Autowired
    private UserFacade userFacade;

    @GetMapping("user/{id}")
    public UserInfo getUser(@PathVariable("id") String id) {
        return userFacade.getUser(id);
    }

    @GetMapping("/user/info")
    public String updateUser(String id, String name, Integer age) {
        UserInfo build = UserInfo.builder().id(id).name(name).age(age).build();
        return userFacade.updateUser(build);
    }

}
