package com.duplicall.user.controller;

import com.duplicall.user.services.user.IUser;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description UserController
 * @Author Sean
 * @Date 2021/8/23 9:47
 * @Version 1.0
 */
@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private IUser userService;

    @GetMapping("time")
    public String time() {
        return userService.timeOut();
    }

    @GetMapping("exception")
    public String exception(String id) {
        return userService.exception(id);
    }
}
