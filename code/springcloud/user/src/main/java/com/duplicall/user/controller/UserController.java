package com.duplicall.user.controller;

import com.common.UserInfo;
import com.duplicall.user.services.user.IUser;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description UserController
 * @Author Sean
 * @Date 2021/8/23 9:47
 * @Version 1.0
 */
@RestController
@RequestMapping("user")
public class UserController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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

    @GetMapping("info/{id}")
    public UserInfo info(@PathVariable(name = "id") String id) {
        logger.info("user id [{}]", id);
        return UserInfo.builder().id(id).name("Sean").age(20).build();
    }

    @PutMapping("info")
    public String userInfo(@RequestBody UserInfo userInfo) {
        logger.info("userInfo [{}]", userInfo.toString());
        return "update user info";
    }

    @GetMapping("userList")
    public List<UserInfo> getUserList(@RequestParam("ids") String[] ids) {
        ArrayList<UserInfo> userInfos = new ArrayList<>();
        for (String id : ids) {
            userInfos.add(UserInfo.builder().id(id).name("sean" + id).age(20).build());
        }
        return userInfos;
    }

    /**
     * 使用请求头传递参数
     *
     * @param id
     * @return
     */
    @DeleteMapping("/info")
    public String delete(@RequestHeader("id") String id) {
        return "delete id " + id + "success";
    }

    /**
     * 传递文件
     * @param file
     * @return
     */
    @PostMapping(value = "/upload")
    public String upload(@RequestPart("file") MultipartFile file) {
        logger.info("file size [{}]", file.getSize());
        return "upload success ";
    }
}
