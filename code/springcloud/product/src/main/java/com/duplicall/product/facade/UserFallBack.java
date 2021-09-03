package com.duplicall.product.facade;

import com.common.UserInfo;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description UserFallBack
 * @Author Sean
 * @Date 2021/9/3 14:50
 * @Version 1.0
 */

/**
 * 需要类提供服务降级方法，需要满足三个条件
 * 1. 实现openFeign  接口定义的方法
 * 2. 将Bean 注册为spring bean
 * 3. 使用 @FeignClient 的 fallback 配置项指向当前类
 */
@Component
public class UserFallBack implements UserFacade {
    @Override
    public UserInfo getUser(String id) {
        return UserInfo.builder().name(null).age(0).id(null).build();
    }

    @Override
    public String updateUser(UserInfo userInfo) {
        return "服务降级";
    }

    @Override
    public List<UserInfo> userList(String[] ids) {
        return new ArrayList<>();
    }

    @Override
    public String delete(String id) {
        return "服务降级";
    }

    @Override
    public String upload(MultipartFile file) {
        return "服务降级";
    }
}
