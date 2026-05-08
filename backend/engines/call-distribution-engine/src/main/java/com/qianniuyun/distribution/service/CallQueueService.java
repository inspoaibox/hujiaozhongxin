package com.qianniuyun.distribution.service;

import com.qianniuyun.distribution.model.CallQueueItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * 呼叫队列服务
 * 使用 Redis ZSet 实现优先级队列（按入队时间排序）
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CallQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String QUEUE_KEY_PREFIX = "call:queue:";
    private static final int QUEUE_TIMEOUT_SECONDS = 300;

    /**
     * 呼叫入队
     */
    public void enqueue(String callId, String skillGroupCode, boolean isVip) {
        String queueKey = QUEUE_KEY_PREFIX + skillGroupCode;

        // VIP 呼叫使用负时间戳，确保排在前面
        double score = isVip
                ? -(double) System.currentTimeMillis()
                : (double) System.currentTimeMillis();

        redisTemplate.opsForZSet().add(queueKey, callId, score);

        // 记录入队时间（用于超时检查）
        redisTemplate.opsForHash().put("call:queue:meta", callId,
                LocalDateTime.now().toString() + ":" + skillGroupCode);

        // 更新队列监控数据
        updateQueueMetrics(skillGroupCode);

        log.info("呼叫入队: callId={}, skillGroup={}, isVip={}", callId, skillGroupCode, isVip);
    }

    /**
     * 呼叫出队（取出等待最久的呼叫）
     */
    public CallQueueItem dequeue(String skillGroupCode) {
        String queueKey = QUEUE_KEY_PREFIX + skillGroupCode;

        Set<Object> callIds = redisTemplate.opsForZSet().range(queueKey, 0, 0);
        if (callIds == null || callIds.isEmpty()) return null;

        String callId = (String) callIds.iterator().next();
        redisTemplate.opsForZSet().remove(queueKey, callId);

        // 获取入队时间
        String meta = (String) redisTemplate.opsForHash().get("call:queue:meta", callId);
        redisTemplate.opsForHash().delete("call:queue:meta", callId);

        long waitSeconds = 0;
        if (meta != null) {
            String[] parts = meta.split(":");
            LocalDateTime enqueueTime = LocalDateTime.parse(parts[0]);
            waitSeconds = ChronoUnit.SECONDS.between(enqueueTime, LocalDateTime.now());
        }

        updateQueueMetrics(skillGroupCode);

        return CallQueueItem.builder()
                .callId(callId)
                .skillGroupCode(skillGroupCode)
                .waitSeconds(waitSeconds)
                .build();
    }

    /**
     * 获取队列长度
     */
    public long getQueueSize(String skillGroupCode) {
        Long size = redisTemplate.opsForZSet().size(QUEUE_KEY_PREFIX + skillGroupCode);
        return size != null ? size : 0;
    }

    /**
     * 定时检查队列超时（每30秒执行）
     */
    @Scheduled(fixedDelay = 30000)
    public void checkQueueTimeout() {
        // 获取所有队列中的呼叫元数据
        Map<Object, Object> allMeta = redisTemplate.opsForHash().entries("call:queue:meta");

        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Object, Object> entry : allMeta.entrySet()) {
            String callId = (String) entry.getKey();
            String meta = (String) entry.getValue();

            if (meta == null) continue;

            String[] parts = meta.split(":");
            LocalDateTime enqueueTime = LocalDateTime.parse(parts[0]);
            long waitSeconds = ChronoUnit.SECONDS.between(enqueueTime, now);

            if (waitSeconds >= QUEUE_TIMEOUT_SECONDS) {
                log.warn("呼叫队列超时: callId={}, waitSeconds={}", callId, waitSeconds);
                // 发布超时事件，播放道歉语音并提供留言选项
                kafkaTemplate.send("qianniu.call.queue.timeout", callId,
                        Map.of("callId", callId, "waitSeconds", waitSeconds));
            }
        }
    }

    /**
     * 更新队列监控指标
     */
    private void updateQueueMetrics(String skillGroupCode) {
        long queueSize = getQueueSize(skillGroupCode);

        // 超过阈值发送预警
        if (queueSize > 10) {
            log.warn("队列积压预警: skillGroup={}, size={}", skillGroupCode, queueSize);
            kafkaTemplate.send("qianniu.queue.alert", skillGroupCode,
                    Map.of("skillGroupCode", skillGroupCode, "queueSize", queueSize));
        }
    }
}
