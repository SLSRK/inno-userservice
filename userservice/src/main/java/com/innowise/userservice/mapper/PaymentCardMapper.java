package com.innowise.userservice.mapper;

import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(source = "user.id", target = "userId")
    PaymentCardResponseDto toDto(PaymentCard paymentCard);

    PaymentCard toEntity(PaymentCardUpdateDto paymentCardUpdateDto);

    PaymentCard toEntityWithUser(PaymentCardCreateDto paymentCardCreateDto);
}
