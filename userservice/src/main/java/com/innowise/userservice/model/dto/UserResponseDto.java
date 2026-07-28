package com.innowise.userservice.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class UserResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String surname;

    private LocalDate birthDate;

    private Boolean active;

    private String email;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<PaymentCardResponseDto> paymentCards = new ArrayList<>();
}
