package com.rikkeibank.staffservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;
    @NotBlank(message = "Email is required") @Email
    private String email;
    private String phone;
    @NotBlank(message = "Position is required")
    private String position;
    private String department;
}
