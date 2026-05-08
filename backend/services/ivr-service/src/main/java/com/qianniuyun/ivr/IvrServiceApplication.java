package com.qianniuyun.ivr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ivr-service 启动类
 * 作者：深圳市千牛云科技有限公司
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class IvrServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IvrServiceApplication.class, args);
    }
}
