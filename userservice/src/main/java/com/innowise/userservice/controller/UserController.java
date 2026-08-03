package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PaymentCardService paymentCardService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserRequestDto userRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(userRequestDto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or authentication.principal == #id")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String surname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(name, surname, page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN') or authentication.principal == #id")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id,
                                                      @Valid @RequestBody UserRequestDto userRequestDto) {
        return ResponseEntity.ok(userService.updateUser(id, userRequestDto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<UserResponseDto> setUserActive(@PathVariable Long id,
                                                         @RequestParam Boolean isActive) {
        return ResponseEntity.ok(userService.setUserActive(id, isActive));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }

    @PostMapping("{userId}/cards")
    @PreAuthorize("hasAuthority('ADMIN') or authentication.principal == #userId")
    public ResponseEntity<PaymentCardResponseDto> createPaymentCard(@PathVariable Long userId,
                                                                    @Valid @RequestBody PaymentCardCreateDto paymentCardCreateDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentCardService.createPaymentCard(userId,paymentCardCreateDto));
    }

    @GetMapping("{userId}/cards")
    @PreAuthorize("hasAuthority('ADMIN') or authentication.principal == #userId")
    public ResponseEntity<List<PaymentCardResponseDto>> getAllPaymentCardsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(paymentCardService.getAllPaymentCardsByUserId(userId));
    }
}
