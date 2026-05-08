package com.qianniuyun.auth.service;

import com.qianniuyun.auth.entity.AuditLog;
import com.qianniuyun.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 操作审计日志服务
 * 作者：深圳市千牛云科技有限公司
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * 异步记录审计日志
     */
    @Async
    public void log(Long userId, String action, String detail, String ipAddress) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setAction(action);
            auditLog.setDetail(detail);
            auditLog.setIpAddress(ipAddress);
            auditLog.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("记录审计日志失败: userId={}, action={}", userId, action, e);
        }
    }
}
