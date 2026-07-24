package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PaymentCardUpdateDto {

    @NotBlank(message = "Cannot be empty")
    @Size(min = 16, max = 16, message = "Card number must contain 16 characters")
    private String number;

    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;

    @NotNull(message = "ID cannot be null")
    private Long userId;
}
