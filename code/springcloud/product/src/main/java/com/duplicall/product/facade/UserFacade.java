package com.duplicall.product.facade;

import com.common.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description UserFacade
 * @Author Sean
 * @Date 2021/9/2 9:58
 * @FeignClient("user") 表示接口是一个OpenFeign 的客户端 user 是微服务的名称，指向用户微服务。
 * @Version 1.0
 */
@FeignClient(value = "user", fallbackFactory = UserFallBackFactory.class)
public interface UserFacade {
    @GetMapping("/user/info/{id}")
    UserInfo getUser(@PathVariable("id") String id);

    @PutMapping("/user/info")
    String updateUser(@RequestBody UserInfo userInfo);

    @GetMapping("user/userList")
    List<UserInfo> userList(@RequestParam("ids") String[] ids);

    @DeleteMapping("/user/info")
    String delete(@RequestHeader("id") String id);

    /**
     * 提交一个 multipart / from-data 类型的表单
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "user/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String upload(@RequestPart("file") MultipartFile file);

}
