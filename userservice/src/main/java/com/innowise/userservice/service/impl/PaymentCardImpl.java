package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.CardsQuantityException;
import com.innowise.userservice.exception.NotActiveException;
import com.innowise.userservice.exception.NotFoundException;
import com.innowise.userservice.mapper.PaymentCardMapper;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.entity.PaymentCard;
import com.innowise.userservice.model.entity.User;
import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.PaymentCardService;
import com.innowise.userservice.specification.PaymentCardSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCardImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public PaymentCardResponseDto createPaymentCard(Long userId, PaymentCardCreateDto paymentCardCreateDto) {
        PaymentCard paymentCard = paymentCardMapper.toEntityWithUser(paymentCardCreateDto);
        User user = userRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        if(user.getPaymentCards().size() >= 5) {
            throw new CardsQuantityException("The user already has 5 cards");
        }

        paymentCard.setUser(user);
        paymentCard.setHolder(user.getName() + " " + user.getSurname());
        paymentCard.setActive(user.getActive());
        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    public PaymentCardResponseDto getPaymentCardById(Long id) {
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment card not found"));

        if(!paymentCard.getActive()) {
            throw new NotActiveException("Payment card not active");
        }
        return paymentCardMapper.toDto(paymentCard);
    }

    public Page<PaymentCardResponseDto> getAllPaymentCards(
            String holder,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Specification<PaymentCard> spec = Specification
                .where(PaymentCardSpecification.byHolder(holder));

        return paymentCardRepository.findAll(spec, pageable)
                .map(paymentCardMapper::toDto);
    }

    public List<PaymentCardResponseDto> getAllPaymentCardsByUserId(Long userId) {

        List<PaymentCard> paymentCards = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"))
                .getPaymentCards();

        return paymentCards.stream()
                .filter(PaymentCard :: getActive)
                .map(paymentCardMapper :: toDto)
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#paymentCardUpdateDto.userId"),
            @CacheEvict(value = "users", key = "#id")
    })
    public PaymentCardResponseDto updatePaymentCard(Long id, PaymentCardUpdateDto paymentCardUpdateDto) {
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment card not found"));

        if(paymentCardUpdateDto.getUserId() != paymentCard.getUser().getId()) {
            User user = userRepository.findById(paymentCardUpdateDto.getUserId())
                    .orElseThrow(() -> new NotFoundException("User not found"));
            if(user.getPaymentCards().size() >= 5) {
                throw new CardsQuantityException("The user already has 5 cards");
            }
            paymentCard.setUser(user);
            paymentCard.setHolder(user.getName() + " " + user.getSurname());
        }
        PaymentCard newPaymentCard = paymentCardMapper.toEntity(paymentCardUpdateDto);
        paymentCard.setNumber(newPaymentCard.getNumber());
        paymentCard.setExpirationDate(newPaymentCard.getExpirationDate());

        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardResponseDto setPaymentCardActive(Long id, Boolean isActive){
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment card not found"));

        if(!paymentCard.getUser().getActive()) {
            throw new NotActiveException("Cannot activate a card when the holder's profile is not active");
        }

        paymentCard.setActive(isActive);
        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }
}
