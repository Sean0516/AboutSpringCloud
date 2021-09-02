package com.common;

import lombok.Builder;
import lombok.Data;

/**
 * @Description UserInfo
 * @Author Sean
 * @Date 2021/9/2 9:38
 * @Version 1.0
 */
@Data
@Builder
public class UserInfo {
    private String name;
    private String id;
    private int age;

}
