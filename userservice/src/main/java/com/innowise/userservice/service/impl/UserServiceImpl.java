package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.AlreadyExistsException;
import com.innowise.userservice.exception.NotActiveException;
import com.innowise.userservice.exception.NotFoundException;
import com.innowise.userservice.mapper.UserMapper;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.UserService;
import com.innowise.userservice.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        log.debug("Creating a new user: name={}, surname={}, birth_date={}, email={}",
                userRequestDto.getName(),
                userRequestDto.getSurname(),
                userRequestDto.getBirthDate(),
                userRequestDto.getEmail());
        if(checkEmailForExistence(userRequestDto.getEmail())) {
            throw new AlreadyExistsException("This email is taken.");
        }
        User user = userMapper.toEntity(userRequestDto);
        user.setActive(true);
        log.debug("The data is valid, the user is about to be created...");
        return userMapper.toDto(userRepository.save(user));
    }

    @Cacheable(value = "users", key = "#id")
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found."));

        if(!user.getActive()) {
            throw new NotActiveException("User's profile not active.");
        }

        return userMapper.toDto(user);
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found."));
    }

    public Page<UserResponseDto> getAllUsers(String name, String surname, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = Specification
                .where(UserSpecification.byName(name))
                .and(UserSpecification.bySurname(surname));

        return userRepository.findAll(spec, pageable)
                .map(userMapper :: toDto);
    }

    public List<UserResponseDto> getUsersByIds(List<Long> ids){
        return userRepository.findAllById(ids).stream()
                .filter(u -> u.getActive())
                .map(userMapper :: toDto)
                .distinct()
                .collect(Collectors.toCollection(ArrayList:: new));
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found."));
        if(!user.getActive()) {
            throw new NotActiveException("User's profile not active.");
        }
        return userMapper.toDto(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
        log.debug("Updating the user with the id:{}. New info: name={}, surname={}, birth_date={}, email={}",
                id,
                userRequestDto.getName(),
                userRequestDto.getSurname(),
                userRequestDto.getBirthDate(),
                userRequestDto.getEmail());
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found."));

        User newUser = userMapper.toEntity(userRequestDto);

        user.getPaymentCards().forEach(paymentCard -> {
            paymentCard.setHolder(newUser.getName() + " " + newUser.getSurname());
            log.debug("User's card with the id={} changes it's holder into holder={}",
                    paymentCard.getId(),
                    paymentCard.getHolder());
        });

        user.setName(newUser.getName());
        user.setSurname(newUser.getSurname());
        user.setBirthDate(newUser.getBirthDate());
        if (!user.getEmail().equals(newUser.getEmail()) && checkEmailForExistence(userRequestDto.getEmail())) {
            throw new AlreadyExistsException("This email is taken.");
        }
        user.setEmail(newUser.getEmail());

        log.debug("The data is valid, the user is about to be updated...");
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public UserResponseDto setUserActive(Long id, Boolean isActive) {
        log.debug("Changing the state of user with the id={}, to activity status={}",
                id,
                isActive);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found."));

        user.setActive(isActive);
        if(!isActive) {
            user.getPaymentCards().forEach(paymentCard -> {
                paymentCard.setActive(false);
                log.debug("User's card with the id={} switches it's activity status into active=false",
                        paymentCard.getId());
            });
        }
        log.debug("The activity of the user is about to be changed...");
        return userMapper.toDto(userRepository.save(user));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
        log.debug("A user with the id={} successfully deleted", id);
    }

    private Boolean checkEmailForExistence(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}
