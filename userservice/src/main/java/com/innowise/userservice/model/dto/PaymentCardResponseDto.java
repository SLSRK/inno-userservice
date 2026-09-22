package com.innowise.userservice.model.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PaymentCardResponseDto (

        Long id,

        String number,

        String holder,

        LocalDate expirationDate,

        Boolean active,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        Long userId

) implements Serializable {
    private static final long serialVersionUID = 1L;
}
