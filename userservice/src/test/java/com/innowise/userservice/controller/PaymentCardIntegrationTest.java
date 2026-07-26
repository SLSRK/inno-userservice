package com.innowise.userservice.controller;

import com.innowise.userservice.UserserviceApplication;
import com.innowise.userservice.model.dto.PaymentCardUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = UserserviceApplication.class)
@AutoConfigureMockMvc
public class PaymentCardIntegrationTest extends IntegrationTestCommons{

    @Test
    void getPaymentCardById_shouldReturnCard_whenActive() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        mockMvc.perform(get("/api/cards/{cardId}", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getPaymentCardById_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/cards/{cardId}", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentCardById_shouldReturnBadRequest_whenCardIsNotActive() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cards/{cardId}", cardId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPaymentCards_shouldFilterByHolder() throws Exception {
        String name = "Holder-" + UUID.randomUUID();
        Long userId = createUser(name, "Surname");
        Long cardId = createCard(userId);

        mockMvc.perform(get("/api/cards").param("holder", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(cardId));
    }

    @Test
    void getAllPaymentCards_shouldReturnEmptyPage_whenNoMatch() throws Exception {
        mockMvc.perform(get("/api/cards")
                        .param("holder", "definitely-not-existing-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void updatePaymentCard_shouldUpdateNumberAndExpirationDate() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(paymentCardUpdateDto.getNumber()))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void updatePaymentCard_shouldMoveCardToAnotherUser() throws Exception {
        Long owner = createUser("Ivan", "Slesarenko");
        Long newOwner = createUser("Alex", "Petrov");
        Long cardId = createCard(owner);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(newOwner);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(3));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(newOwner))
                .andExpect(jsonPath("$.holder").value("Alex Petrov"));
    }

    @Test
    void updatePaymentCard_shouldReturnBadRequest_whenNewOwnerAlreadyHasFiveCards() throws Exception {
        Long oldOwner = createUser("Ivan", "Slesarenko");
        Long fullOwner = createUser("Alex", "Petrov");
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
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void updatePaymentCard_shouldReturnBadRequest_whenExpirationDateInPast() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().minusDays(1));

        mockMvc.perform(put("/api/cards/{cardId}", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    void updatePaymentCard_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

        PaymentCardUpdateDto paymentCardUpdateDto = new PaymentCardUpdateDto();
        paymentCardUpdateDto.setUserId(userId);
        paymentCardUpdateDto.setNumber(createCardNumber());
        paymentCardUpdateDto.setExpirationDate(LocalDate.now().plusYears(1));

        mockMvc.perform(put("/api/cards/{cardId}", 999_999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardUpdateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setPaymentCardActive_shouldDeactivateCard() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void setPaymentCardActive_shouldReturnBadRequest_whenHolderIsNotActive() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        Long cardId = createCard(userId);

        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/cards/{cardId}", cardId).param("isActive", "true"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setPaymentCardActive_shouldReturnNotFound_whenCardDoesNotExist() throws Exception {
        mockMvc.perform(patch("/api/cards/{cardId}", 999_999_999L).param("isActive", "true"))
                .andExpect(status().isNotFound());
    }
}
