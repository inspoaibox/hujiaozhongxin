package com.qianniuyun.auth.repository;

import com.qianniuyun.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
