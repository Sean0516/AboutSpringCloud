package com.duplicall.product.facade;

import com.common.UserInfo;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description UserFallBackFactory
 * @Author Sean
 * @Date 2021/9/3 15:02
 * @Version 1.0
 */
@Component
public class UserFallBackFactory implements FallbackFactory<UserFacade> {
    @Override
    public UserFacade create(Throwable throwable) {
        return new UserFacade() {
            @Override
            public UserInfo getUser(String id) {
                return UserInfo.builder().name("demo").build();
            }

            @Override
            public String updateUser(UserInfo userInfo) {
                return throwable.getMessage();
            }

            @Override
            public List<UserInfo> userList(String[] ids) {
                return null;
            }

            @Override
            public String delete(String id) {
                return throwable.getMessage();
            }

            @Override
            public String upload(MultipartFile file) {
                return throwable.getMessage();
            }
        };
    }
}
