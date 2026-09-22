package com.innowise.userservice.service;

import com.innowise.userservice.exception.NotActiveException;
import com.innowise.userservice.exception.NotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_shouldCreateUser() {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name("Ivan")
                .surname("Slesarenko")
                .birthDate(LocalDate.of(2001, 6, 25))
                .build();
        User user = new User();
        User saved = currentUser();
        UserResponseDto userResponseDto = UserResponseDto.builder().build();

        when(userMapper.toEntity(userRequestDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(userResponseDto);

        UserResponseDto result = userService.createUser(userRequestDto);
        assertEquals(userResponseDto, result);
        assertTrue(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void getUserById_shouldReturnUser() {
        User user = currentUser();
        UserResponseDto userResponseDto = UserResponseDto.builder().build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userResponseDto);

        UserResponseDto result = userService.getUserById(1L);
        assertEquals(userResponseDto, result);
    }


    @Test
    void getUserById_shouldThrowWhenNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void getUserById_shouldThrowWhenInactive() {
        User user = currentUser();
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThrows(NotActiveException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void updateUser_shouldUpdateFields() {
        User user = currentUser();

        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name("Jan")
                .surname("Slesarensky")
                .birthDate(LocalDate.now())
                .email(user.getEmail())
                .build();
        User newUser = newUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toEntity(userRequestDto)).thenReturn(newUser);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(UserResponseDto.builder().build());

        userService.updateUser(1L, userRequestDto);

        assertEquals("Jan", user.getName());
        assertEquals("Slesarensky", user.getSurname());
    }

    @Test
    void updateUser_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> userService.updateUser(1L, UserRequestDto.builder().build())
        );
    }

    @Test
    void setUserActive_shouldDisableCards() {
        User user = new User();
        user.setActive(true);
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setActive(true);
        user.getPaymentCards().add(paymentCard);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);
        when(userMapper.toDto(user))
                .thenReturn(UserResponseDto.builder().build());

        userService.setUserActive(1L,false);
        assertFalse(user.getActive());
        assertFalse(paymentCard.getActive());
    }

    @Test
    void updateUser_shouldUpdateCardHolder() {
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setHolder("Ivan Slesarenko");

        User user = currentUser();
        user.getPaymentCards().add(paymentCard);
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name("Jan")
                .surname("Slesarensky")
                .build();
        User newUser = newUser();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userMapper.toEntity(userRequestDto))
                .thenReturn(newUser);
        when(userRepository.save(user))
                .thenReturn(user);
        when(userMapper.toDto(user))
                .thenReturn(UserResponseDto.builder().build());

        userService.updateUser(1L, userRequestDto);
        assertEquals("Jan Slesarensky", paymentCard.getHolder());
    }

    @Test
    void setUserActive_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> userService.setUserActive(1L, true)
        );
    }

    @Test
    void setUserActive_shouldActivateWithoutChangingCards() {
        User user = new User();
        user.setActive(false);
        PaymentCard paymentCard = new PaymentCard();
        paymentCard.setActive(false);
        user.getPaymentCards().add(paymentCard);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);
        when(userMapper.toDto(user))
                .thenReturn(UserResponseDto.builder().build());
        userService.setUserActive(1L, true);
        assertTrue(user.getActive());
        assertFalse(paymentCard.getActive());
    }

    private User currentUser() {
        User user = new User();
        user.setName("Ivan");
        user.setSurname("Slesarenko");
        user.setEmail("ivan@anymail.com");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setActive(true);
        user.setPaymentCards(new ArrayList<>());
        return user;
    }

    private User newUser() {
        User user = new User();
        user.setName("Jan");
        user.setSurname("Slesarensky");
        user.setEmail("jan@anymail.com");
        user.setBirthDate(LocalDate.of(2000, 1, 1));
        user.setActive(true);
        user.setPaymentCards(new ArrayList<>());
        return user;
    }
}