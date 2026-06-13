package com.foodzie.auth_service.service;

import com.foodzie.auth_service.data.AuditLog;
import com.foodzie.auth_service.data.AuditLog.AuditStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AuditService {

    void log(Long userId, String actorEmail, String action, String resource,
             String resourceId, String ipAddress, String userAgent,
             AuditStatus status, Map<String, Object> details);

    Page<AuditLog> getLogsByUser(Long userId, Pageable pageable);

    Page<AuditLog> getLogsByAction(String action, Pageable pageable);
}
