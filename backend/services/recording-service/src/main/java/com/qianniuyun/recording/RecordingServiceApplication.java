package com.qianniuyun.recording;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.qianniuyun.common.exception.GlobalExceptionHandler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * recording-service 启动类
 * 作者：深圳市千牛云科技有限公司
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
@EnableAsync
@EnableScheduling
public class RecordingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecordingServiceApplication.class, args);
    }
}
