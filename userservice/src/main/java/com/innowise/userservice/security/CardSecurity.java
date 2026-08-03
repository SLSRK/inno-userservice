package com.innowise.userservice.security;

import com.innowise.userservice.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("cardSecurity")
@RequiredArgsConstructor
public class CardSecurity {

    private final PaymentCardRepository paymentCardRepository;

    public boolean isOwner(Long cardId, Long userId) {
        return paymentCardRepository.findById(cardId)
                .map(card -> card.getUser().getId().equals(userId))
                .orElse(false);
    }
}