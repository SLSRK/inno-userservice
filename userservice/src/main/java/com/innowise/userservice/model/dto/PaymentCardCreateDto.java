package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PaymentCardCreateDto {

    @NotBlank(message = "Cannot be empty")
    @Size(min = 16, max = 16, message = "Card number must contain 16 characters")
    private String number;

    @Future(message = "Expiration date must be in the future")
    private LocalDate expirationDate;
}