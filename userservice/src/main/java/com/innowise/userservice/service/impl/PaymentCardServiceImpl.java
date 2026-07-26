package com.innowise.userservice.service.impl;

import com.innowise.userservice.exception.AlreadyExistsException;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCardServiceImpl implements PaymentCardService {

    private final PaymentCardRepository paymentCardRepository;
    private final UserRepository userRepository;
    private final PaymentCardMapper paymentCardMapper;

    @Value("${user.cards.limit}")
    private int userCardsLimit;

    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public PaymentCardResponseDto createPaymentCard(Long userId, PaymentCardCreateDto paymentCardCreateDto) {
        log.debug("Creating a new card.");
        PaymentCard paymentCard = paymentCardMapper.toEntityWithUser(paymentCardCreateDto);
        setUserInfo(paymentCard, userId);
        paymentCard.setActive(paymentCard.getUser().getActive());

        if(checkNumberForExistence(paymentCard.getNumber())) {
            throw new AlreadyExistsException("This number is taken");
        }

        log.debug("The data is valid, the card is about to be created...");
        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    public PaymentCardResponseDto getPaymentCardById(Long id) {
        log.debug("Getting the card with id:{}", id);
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
        log.debug("Getting the cards that match the given criteria");
        Pageable pageable = PageRequest.of(page, size);

        Specification<PaymentCard> spec = Specification
                .where(PaymentCardSpecification.byHolder(holder));

        return paymentCardRepository.findAll(spec, pageable)
                .map(paymentCardMapper::toDto);
    }

    public List<PaymentCardResponseDto> getAllPaymentCardsByUserId(Long userId) {
        log.debug("Getting the cards, owned by user id:{}", userId);
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
            @CacheEvict(value = "users", key = "#result.userId")
    })
    public PaymentCardResponseDto updatePaymentCard(Long id, PaymentCardUpdateDto paymentCardUpdateDto) {
        log.debug("Updating the card with the id:{}", id);
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment card not found"));

        if(!paymentCardUpdateDto.getUserId().equals(paymentCard.getUser().getId())) {
            setUserInfo(paymentCard, paymentCardUpdateDto.getUserId());
        }
        PaymentCard newPaymentCard = paymentCardMapper.toEntity(paymentCardUpdateDto);
        if(!paymentCard.getNumber().equals(newPaymentCard.getNumber()) && checkNumberForExistence(newPaymentCard.getNumber())) {
            throw new AlreadyExistsException("This number is taken");
        }
        paymentCard.setNumber(newPaymentCard.getNumber());
        paymentCard.setExpirationDate(newPaymentCard.getExpirationDate());

        log.debug("The data is valid, the card is about to be updated...");
        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#result.userId")
    public PaymentCardResponseDto setPaymentCardActive(Long id, Boolean isActive){
        log.debug("Changing the state of card with the id:{}", id);
        PaymentCard paymentCard = paymentCardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment card not found"));

        if(!paymentCard.getUser().getActive()) {
            throw new NotActiveException("Cannot activate a card when the holder's profile is not active");
        }

        paymentCard.setActive(isActive);
        log.debug("The activity of the card is about to be changed...");
        return paymentCardMapper.toDto(paymentCardRepository.save(paymentCard));
    }

    private void setUserInfo(PaymentCard paymentCard, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if(paymentCardRepository.countByUserId(user.getId()) >= userCardsLimit) {
            throw new CardsQuantityException("The user already has maximum cards");
        }
        paymentCard.setUser(user);
        paymentCard.setHolder(user.getName() + " " + user.getSurname());
        log.debug("Holder info has been successfully filled in");
    }

    private Boolean checkNumberForExistence(String number) {
        return paymentCardRepository.findByNumber(number).isPresent();
    }
}
