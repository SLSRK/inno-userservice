package com.innowise.userservice.controller;

import com.innowise.userservice.UserserviceApplication;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = UserserviceApplication.class,
        properties = "jwt.secret=jwt-secret-for-test-JzdWIiOiI1Iiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3ODU3NTM1MjAsImV4cC"
)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentCardIntegrationTest extends IntegrationTestCommons{

    private static final String NAME = "Ivan";
    private static final String SURNAME = "Slesarenko";
    private static final String NEW_NAME = "Jan";
    private static final String NEW_SURNAME = "Slesarensky";
    private static final Long NON_EXISTENT_ID = 999_999_999L;

    @Test
    void getPaymentCardById_shouldReturnCard_whenActive() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        mockMvc.perform(get("/api/cards/{cardId}", cardId)
                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getPaymentCardById_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}", NON_EXISTENT_ID).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentCardById_shouldReturnBadRequest_whenCardIsNotActive() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "false")
                        .with(admin()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cards/{cardId}", cardId).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPaymentCards_shouldFilterByHolder() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        mockMvc.perform(get("/api/cards").param("holder", NAME).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cardId));
    }

    @Test
    void getAllPaymentCards_shouldReturnEmptyPage_whenNoMatch() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("holder", "definitely-not-existing-" + UUID.randomUUID())
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void updatePaymentCard_shouldUpdateNumberAndExpirationDate() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto))
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(paymentCardUpdateDto.getNumber()))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void updatePaymentCard_shouldMoveCardToAnotherUser() throws Exception {
        Long owner = createUser(NAME, SURNAME);
        Long newOwner = createUser(NEW_NAME, NEW_SURNAME);
        Long cardId = createCard(owner);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(newOwner);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto))
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(newOwner))
                .andExpect(jsonPath("$.holder").value(NEW_NAME + " " + NEW_SURNAME));
    }

    @Test
    void updatePaymentCard_shouldReturnBadRequest_whenNewOwnerAlreadyHasFiveCards() throws Exception {
        Long oldOwner = createUser(NAME, SURNAME);
        Long fullOwner = createUser(NEW_NAME, NEW_SURNAME);
        Long cardId = createCard(oldOwner);
        for (int i = 0; i < userCardsLimit; i++) {
            createCard(fullOwner);
        }

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(fullOwner);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto))
                        .with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void updatePaymentCard_shouldReturnBadRequest_whenExpirationDateInPast() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().minusDays(1));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    void updatePaymentCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(1));

        mockMvc.perform(put("/api/cards/{cardId}", NON_EXISTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto))
                        .with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void setPaymentCardActive_shouldDeactivateCard() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "false")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void setPaymentCardActive_shouldReturnBadRequest_whenHolderIsNotActive() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false")
                        .with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "true")
                        .with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setPaymentCardActive_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/cards/{cardId}", NON_EXISTENT_ID).param("isActive", "true")
                        .with(admin()))
                .andExpect(status().isNotFound());
    }
}
