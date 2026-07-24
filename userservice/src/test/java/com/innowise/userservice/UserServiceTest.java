package com.innowise.userservice;

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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_shouldCreateUser() {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Ivan");
        userRequestDto.setSurname("Slesarenko");
        userRequestDto.setBirthDate(LocalDate.of(2001,06,25));

        User user = new User();
        User saved = new User();
        UserResponseDto response = new UserResponseDto();

        when(userMapper.toEntity(userRequestDto)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(saved);
        when(userMapper.toDto(saved)).thenReturn(response);

        UserResponseDto result = userService.createUser(userRequestDto);
        assertEquals(response, result);
        assertTrue(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    void getUserById_shouldReturnUser() {
        User user = new User();
        user.setActive(true);
        UserResponseDto dto = new UserResponseDto();

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userMapper.toDto(user))
                .thenReturn(dto);

        UserResponseDto result = userService.getUserById(1L);
        assertEquals(dto, result);
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
        User user = new User();
        user.setActive(false);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        assertThrows(NotActiveException.class,
                () -> userService.getUserById(1L));
    }

    @Test
    void updateUser_shouldUpdateFields() {
        User user = new User();
        user.setName("Ivan");
        user.setSurname("Slesarenko");
        user.setPaymentCards(new ArrayList<>());

        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Jan");
        userRequestDto.setSurname("Slesarensky");
        userRequestDto.setBirthDate(LocalDate.now());
        userRequestDto.setEmail("ivan@anymail.com");

        User newUser = new User();
        newUser.setName("Jan");
        newUser.setSurname("Slesarensky");
        newUser.setBirthDate(userRequestDto.getBirthDate());
        newUser.setEmail(userRequestDto.getEmail());

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userMapper.toEntity(userRequestDto))
                .thenReturn(newUser);
        when(userRepository.save(user))
                .thenReturn(user);
        when(userMapper.toDto(user))
                .thenReturn(new UserResponseDto());
        userService.updateUser(1L, userRequestDto);

        assertEquals("Jan", user.getName());
        assertEquals("Slesarensky", user.getSurname());
    }

    @Test
    void setUserActive_shouldDisableCards() {
        User user = new User();
        user.setActive(true);
        PaymentCard card = new PaymentCard();
        card.setActive(true);
        user.getPaymentCards().add(card);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);
        when(userMapper.toDto(user))
                .thenReturn(new UserResponseDto());

        userService.setUserActive(1L,false);
        assertFalse(user.getActive());
        assertFalse(card.getActive());
    }
}