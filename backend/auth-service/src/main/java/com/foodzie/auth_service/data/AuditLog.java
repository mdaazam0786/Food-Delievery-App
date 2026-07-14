package com.foodzie.auth_service.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id;

    private String userId;

    private String actorEmail;

    private String action;

    private String resource;

    private String resourceId;

    private String ipAddress;

    private String userAgent;

    @Builder.Default
    private AuditStatus status = AuditStatus.SUCCESS;

    private Map<String, Object> details;

    @Indexed
    private LocalDateTime createdAt;

    public enum AuditStatus {
        SUCCESS, FAILURE
    }
}
