package com.rikkeibank.accountservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountTypeRequest {
    @NotBlank(message = "Type name is required") private String typeName;
    private String description;
    private Double interestRate;
}
