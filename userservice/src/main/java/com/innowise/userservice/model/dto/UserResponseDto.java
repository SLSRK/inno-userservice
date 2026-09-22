package com.innowise.userservice.model.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record UserResponseDto (

        Long id,

        String name,

        String surname,

        LocalDate birthDate,

        Boolean active,

        String email,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        List<PaymentCardResponseDto> paymentCards
) implements Serializable {

    private static final long serialVersionUID = 1L;

}
