package com.innowise.userservice;

import com.innowise.userservice.exception.CardsQuantityException;
import com.innowise.userservice.exception.NotActiveException;
import com.innowise.userservice.exception.NotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.impl.PaymentCardImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceTest {

    @Mock
    PaymentCardRepository paymentCardRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PaymentCardMapper paymentCardMapper;

    @InjectMocks
    PaymentCardImpl service;


    @Test
    void createPaymentCard_shouldCreateCard() {
        User user = new User();
        user.setName("Ivan");
        user.setSurname("Slesarenko");
        user.setActive(true);
        PaymentCard card = new PaymentCard();
        PaymentCardResponseDto dto = new PaymentCardResponseDto();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(paymentCardMapper.toEntityWithUser(any()))
                .thenReturn(card);
        when(paymentCardRepository.save(card))
                .thenReturn(card);
        when(paymentCardMapper.toDto(card))
                .thenReturn(dto);

        PaymentCardResponseDto result =
                service.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                );
        assertEquals(dto,result);
        assertEquals("Ivan Slesarenko",card.getHolder());
        assertEquals(user,card.getUser());
    }

    @Test
    void createPaymentCard_shouldThrowWhenFiveCards() {
        User user = new User();
        for(int i=0;i<5;i++) {
            user.getPaymentCards().add(new PaymentCard());
        }

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        assertThrows(
                CardsQuantityException.class,
                () -> service.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                )
        );
    }

    @Test
    void getPaymentCardById_shouldReturnCard() {
        PaymentCard card = new PaymentCard();
        card.setActive(true);

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(card));
        when(paymentCardMapper.toDto(card))
                .thenReturn(new PaymentCardResponseDto());
        assertNotNull(service.getPaymentCardById(1L));
    }

    @Test
    void getPaymentCardById_shouldThrow() {
        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> service.getPaymentCardById(1L)
        );
    }

    @Test
    void createPaymentCard_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> service.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                )
        );
    }

    @Test
    void createPaymentCard_shouldCopyUserActiveStatus() {
        User user = new User();
        user.setActive(false);

        PaymentCard card = new PaymentCard();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(paymentCardMapper.toEntityWithUser(any()))
                .thenReturn(card);
        when(paymentCardRepository.save(card))
                .thenReturn(card);
        when(paymentCardMapper.toDto(card))
                .thenReturn(new PaymentCardResponseDto());

        service.createPaymentCard(1L, new PaymentCardCreateDto());

        assertFalse(card.getActive());
    }

    @Test
    void getPaymentCardById_shouldThrowWhenInactive() {
        PaymentCard card = new PaymentCard();
        card.setActive(false);

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(card));
        assertThrows(
                NotActiveException.class,
                () -> service.getPaymentCardById(1L)
        );
    }
}