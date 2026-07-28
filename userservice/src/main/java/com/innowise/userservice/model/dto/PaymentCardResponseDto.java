package com.innowise.userservice.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PaymentCardResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String number;

    private String holder;

    private LocalDate expirationDate;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long userId;
}
