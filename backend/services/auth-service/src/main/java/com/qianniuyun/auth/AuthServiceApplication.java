package com.qianniuyun.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 认证授权服务启动类
 * 作者：深圳市千牛云科技有限公司
 */
@SpringBootApplication
@EnableAsync
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
