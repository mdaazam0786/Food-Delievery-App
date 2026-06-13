package com.foodzie.auth_service.service.implementation;

import com.foodzie.auth_service.data.AuditLog;
import com.foodzie.auth_service.data.AuditLog.AuditStatus;
import com.foodzie.auth_service.repository.AuditLogRepository;
import com.foodzie.auth_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(Long userId, String actorEmail, String action, String resource,
                    String resourceId, String ipAddress, String userAgent,
                    AuditStatus status, Map<String, Object> details) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .actorEmail(actorEmail)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(status)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    @Override
    public Page<AuditLog> getLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<AuditLog> getLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
}
