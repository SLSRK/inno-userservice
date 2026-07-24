package com.innowise.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRequestDto {

    @NotBlank(message = "Cannot be empty")
    @Size(max = 256, message = "The name is too long")
    private String name;

    @NotBlank(message = "Cannot be empty")
    @Size(max = 256, message = "The surname is too long")
    private String surname;

    @PastOrPresent(message = "Birth date cannot be in the future")
    private LocalDate birthDate;

    @Email(message = "Invalid email")
    @NotBlank(message = "Cannot be empty")
    @Size(max = 256, message = "The email is too long")
    private String email;
}
