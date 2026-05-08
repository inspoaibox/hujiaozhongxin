package com.qianniuyun.call;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 呼叫管理服务启动类
 * 作者：深圳市千牛云科技有限公司
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class CallServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CallServiceApplication.class, args);
    }
}
