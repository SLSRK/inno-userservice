package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record PaymentCardCreateDto (

        @NotBlank(message = "Cannot be empty")
        @Size(min = 16, max = 16, message = "Card number must contain 16 characters")
        String number,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}