package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record UserRequestDto (

        @NotBlank(message = "Cannot be empty")
        @Size(max = 256, message = "The name is too long")
        String name,

        @NotBlank(message = "Cannot be empty")
        @Size(max = 256, message = "The surname is too long")
        String surname,

        @PastOrPresent(message = "Birth date cannot be in the future")
        LocalDate birthDate,

        @Email(message = "Invalid email")
        @NotBlank(message = "Cannot be empty")
        @Size(max = 256, message = "The email is too long")
        String email
) {
}
