package com.innowise.userservice.service;

import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaymentCardService {

    /**
     * Create a new card;
     *
     * @param userId ID of a cardholder;
     * @param paymentCardCreateDto data of the card to create;
     * @return returns the result of creating cart.
     */
    PaymentCardResponseDto createPaymentCard(Long userId, PaymentCardCreateDto paymentCardCreateDto);

    /**
     * Get existing card by ID;
     *
     * @param id ID of the card to get;
     * @return returns the card if it exists.
     */
    PaymentCardResponseDto getPaymentCardById(Long id);

    /**
     * Retrieves a paginated list of payment cards with optional filtering;
     *
     * @param holder optional cardholder name to filter results;
     * @param page a number of page with records, that will be returned;
     * @param size the number of records per page (must be > 0);
     * @return the cards, that match the given criteria.
     */
    Page<PaymentCardResponseDto> getAllPaymentCards(
            String holder,
            int page,
            int size
    );

    /**
     * Get all cards owned by a user;
     *
     * @param userId ID of the cardholder;
     * @return returns holder's cards if they exist.
     */
    List<PaymentCardResponseDto> getAllPaymentCardsByUserId(Long userId);

    /**
     * Update a card's data;
     *
     * @param id ID of the card to update;
     * @param paymentCardUpdateDto new data to be uploaded;
     * @return returns the result of updating the card.
     */
    PaymentCardResponseDto updatePaymentCard(Long id, PaymentCardUpdateDto paymentCardUpdateDto);

    /**
     * Activate/deactivate a card;
     *
     * @param id ID of a card;
     * @param isActive new state to be uploaded;
     * @return returns the result of changing the card's state.
     */
    PaymentCardResponseDto setPaymentCardActive(Long id, Boolean isActive);
}
