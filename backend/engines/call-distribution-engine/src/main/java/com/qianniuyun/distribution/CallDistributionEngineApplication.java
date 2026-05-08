package com.qianniuyun.distribution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 呼叫分配引擎启动类
 * 作者：深圳市千牛云科技有限公司
 */
@SpringBootApplication
@EnableScheduling
public class CallDistributionEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(CallDistributionEngineApplication.class, args);
    }
}
