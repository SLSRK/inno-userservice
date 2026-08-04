package com.innowise.userservice.controller;

import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import com.innowise.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService paymentCardService;

    @GetMapping("/{cardId}")
    @PreAuthorize("hasAuthority('ADMIN') or @cardSecurity.isOwner(#cardId, authentication.principal)")
    public ResponseEntity<PaymentCardResponseDto> getPaymentCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(paymentCardService.getPaymentCardById(cardId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Page<PaymentCardResponseDto>> getAllPaymentCards (
            @RequestParam(required = false) String holder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paymentCardService.getAllPaymentCards(holder,page,size));
    }

    @PutMapping("/{cardId}")
    @PreAuthorize("hasAuthority('ADMIN') or " +
            "(@cardSecurity.isOwner(#cardId, authentication.principal) " +
            "and #paymentCardUpdateDto.userId == authentication.principal)")
    public ResponseEntity<PaymentCardResponseDto> updatePaymentCard(@PathVariable Long cardId,
                                                                    @Valid @RequestBody PaymentCardUpdateDto paymentCardUpdateDto) {
        return ResponseEntity.ok(paymentCardService.updatePaymentCard(cardId, paymentCardUpdateDto));
    }

    @PatchMapping("/{cardId}")
    @PreAuthorize("hasAuthority('ADMIN') or !#isActive")
    public ResponseEntity<PaymentCardResponseDto> setPaymentCardActive(@PathVariable Long cardId,
                                                                       @RequestParam Boolean isActive) {
        return ResponseEntity.ok(paymentCardService.setPaymentCardActive(cardId, isActive));
    }
}
