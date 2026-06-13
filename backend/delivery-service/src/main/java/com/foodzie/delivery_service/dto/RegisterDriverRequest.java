package com.foodzie.delivery_service.dto;

import com.foodzie.delivery_service.data.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDriverRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "^\\d{10,15}$", message = "Invalid phone number format (10-15 digits)")
    private String phoneNumber;

    @NotBlank(message = "email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "vehicleType is required")
    private VehicleType vehicleType;

    /** City/zone key for Redis geo partitioning, e.g. "delhi". Defaults to "default". */
    private String cityZone;
}
