package com.innowise.userservice.service;

import com.innowise.userservice.exception.CardsQuantityException;
import com.innowise.userservice.exception.NotActiveException;
import com.innowise.userservice.exception.NotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.service.impl.PaymentCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCardUnitTest {

    @Mock
    PaymentCardRepository paymentCardRepository;

    @Mock
    UserService userService;

    @Mock
    PaymentCardMapper paymentCardMapper;

    @InjectMocks
    PaymentCardServiceImpl paymentCardService;

    private static final Integer USER_CARD_LIMIT = 5;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(paymentCardService, "userCardsLimit", USER_CARD_LIMIT);
    }

    @Test
    void createPaymentCard_shouldCreateCard() {
        User user = currentUser();
        PaymentCard paymentCard = new PaymentCard();
        PaymentCardResponseDto dto = new PaymentCardResponseDto();

        when(userService.getUserEntityById(1L))
                .thenReturn(user);
        when(paymentCardMapper.toEntityWithUser(any()))
                .thenReturn(paymentCard);
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(dto);

        PaymentCardResponseDto result =
                paymentCardService.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                );
        assertEquals(dto,result);
        assertEquals("Ivan Slesarenko",paymentCard.getHolder());
        assertEquals(user,paymentCard.getUser());
    }

    @Test
    void createPaymentCard_shouldThrowWhenFiveCards() {
        User user = currentUser();

        when(userService.getUserEntityById(1L))
                .thenReturn(user);
        when(paymentCardRepository.countByUserId(1L))
                .thenReturn((long) USER_CARD_LIMIT);
        when(paymentCardMapper.toEntityWithUser(any()))
                .thenReturn(new PaymentCard());
        assertThrows(
                CardsQuantityException.class,
                () -> paymentCardService.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                )
        );
    }

    @Test
    void createPaymentCard_shouldThrowWhenUserNotFound() {
        when(userService.getUserEntityById(1L))
                .thenThrow(new NotFoundException("User not found."));
        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.createPaymentCard(
                        1L,
                        new PaymentCardCreateDto()
                )
        );
    }

    @Test
    void createPaymentCard_shouldCopyUserActiveStatus() {
        User user = currentUser();
        user.setActive(false);

        PaymentCard paymentCard = new PaymentCard();

        when(userService.getUserEntityById(1L))
                .thenReturn(user);
        when(paymentCardMapper.toEntityWithUser(any()))
                .thenReturn(paymentCard);
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(new PaymentCardResponseDto());

        paymentCardService.createPaymentCard(1L, new PaymentCardCreateDto());

        assertFalse(paymentCard.getActive());
    }

    @Test
    void getPaymentCardById_shouldReturnCard() {
        PaymentCard paymentCard = currentCard();

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(new PaymentCardResponseDto());
        assertNotNull(paymentCardService.getPaymentCardById(1L));
    }

    @Test
    void getPaymentCardById_shouldThrow() {
        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> paymentCardService.getPaymentCardById(1L)
        );
    }

    @Test
    void getPaymentCardById_shouldThrowWhenInactive() {
        PaymentCard paymentCard = currentCard();
        paymentCard.setActive(false);

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(paymentCard));
        assertThrows(
                NotActiveException.class,
                () -> paymentCardService.getPaymentCardById(1L)
        );
    }

    @Test
    void updatePaymentCard_shouldUpdateNumberAndDate_whenOwnerUnchanged() {
        User user = currentUser();
        user.setId(1L);

        PaymentCard paymentCard = currentCard();
        paymentCard.setUser(user);
        paymentCard.setExpirationDate(LocalDate.now().plusYears(1));

        PaymentCardUpdateDto updateDto = new PaymentCardUpdateDto();
        updateDto.setUserId(1L);
        updateDto.setNumber("9999888877776666");
        updateDto.setExpirationDate(LocalDate.now().plusYears(2));

        PaymentCard mapped = new PaymentCard();
        mapped.setNumber(updateDto.getNumber());
        mapped.setExpirationDate(updateDto.getExpirationDate());

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardMapper.toEntity(updateDto))
                .thenReturn(mapped);
        when(paymentCardRepository.findByNumber("9999888877776666"))
                .thenReturn(Optional.empty());
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(new PaymentCardResponseDto());

        paymentCardService.updatePaymentCard(1L, updateDto);

        assertEquals("9999888877776666", paymentCard.getNumber());
        assertEquals(updateDto.getExpirationDate(), paymentCard.getExpirationDate());
        assertEquals(user, paymentCard.getUser());
    }

    @Test
    void setPaymentCardActive_shouldDeactivateCard() {
        User user = currentUser();
        PaymentCard paymentCard = currentCard();
        paymentCard.setUser(user);

        when(paymentCardRepository.findById(1L))
                .thenReturn(Optional.of(paymentCard));
        when(paymentCardRepository.save(paymentCard))
                .thenReturn(paymentCard);
        when(paymentCardMapper.toDto(paymentCard))
                .thenReturn(new PaymentCardResponseDto());
        paymentCardService.setPaymentCardActive(1L, false);

        assertFalse(paymentCard.getActive());
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnOnlyActiveCards() {
        User user = currentUser();
        PaymentCard activeCard = currentCard();
        PaymentCard inactiveCard = currentCard();
        inactiveCard.setActive(false);
        user.setPaymentCards(List.of(activeCard, inactiveCard));

        when(userService.getUserEntityById(1L))
                .thenReturn(user);
        when(paymentCardMapper.toDto(activeCard))
                .thenReturn(new PaymentCardResponseDto());

        List<PaymentCardResponseDto> result =
                paymentCardService.getAllPaymentCardsByUserId(1L);

        assertEquals(1, result.size());
    }

    private User currentUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Ivan");
        user.setSurname("Slesarenko");
        user.setActive(true);
        return user;
    }

    private PaymentCard currentCard() {
        PaymentCard paymentCard = new PaymentCard();;
        paymentCard.setNumber("1111222233334444");
        paymentCard.setActive(true);
        return paymentCard;
    }
}