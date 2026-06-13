package com.foodzie.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleAssignRequest {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
