package com.innowise.userservice.controller;

import com.innowise.userservice.UserserviceApplication;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.PaymentCardResponseDto;
import com.innowise.userservice.model.dto.UserRequestDto;
import com.innowise.userservice.model.dto.UserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UserserviceApplication.class)
@AutoConfigureMockMvc
class UserIntegrationTest extends IntegrationTestCommons {

    private static final String NAME = "Ivan";
    private static final String SURNAME = "Slesarenko";
    private static final String NEW_NAME = "Jan";
    private static final String NEW_SURNAME = "Slesarensky";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate NEW_BIRTH_DATE = LocalDate.of(1999, 1, 1);
    private static final Long NON_EXISTENT_ID = 999_999_999L;
    private static final String TEST_EMAIL_DOMAIN = "@test.com";

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName(NAME);
        userRequestDto.setSurname(SURNAME);
        userRequestDto.setBirthDate(BIRTH_DATE);
        userRequestDto.setEmail(UUID.randomUUID() + TEST_EMAIL_DOMAIN);

        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(NAME, actual.getName());
        assertEquals(SURNAME, actual.getSurname());
        assertTrue(actual.getActive());
        assertNotNull(actual.getId());
        assertTrue(actual.getPaymentCards().isEmpty());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("");
        userRequestDto.setSurname(SURNAME);
        userRequestDto.setBirthDate(LocalDate.of(2000, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID() + TEST_EMAIL_DOMAIN);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName(NAME);
        userRequestDto.setSurname(SURNAME);
        userRequestDto.setBirthDate(LocalDate.of(2000, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenBirthDateInFuture() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName(NAME);
        userRequestDto.setSurname(SURNAME);
        userRequestDto.setBirthDate(LocalDate.now().plusDays(1));
        userRequestDto.setEmail(UUID.randomUUID() + TEST_EMAIL_DOMAIN);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.birthDate").exists());
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        String response = mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(userId, actual.getId());
        assertEquals(NAME, actual.getName());
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_shouldReturnBadRequest_whenUserIsNotActive() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_shouldFilterByNameAndSurname() throws Exception {
        Long userId = createUser(NEW_NAME, NEW_SURNAME);

        mockMvc.perform(get("/api/users")
                        .param("name", NEW_NAME)
                        .param("surname", NEW_SURNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(userId));
    }

    @Test
    void getAllUsers_shouldReturnEmptyPage_whenNoMatch() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("name", "definitely-not-existing-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void updateUser_shouldUpdateFields() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName(NEW_NAME);
        userRequestDto.setSurname(NEW_SURNAME);
        userRequestDto.setBirthDate(NEW_BIRTH_DATE);
        userRequestDto.setEmail(UUID.randomUUID() + TEST_EMAIL_DOMAIN);

        String response = mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(NEW_NAME, actual.getName());
        assertEquals(NEW_SURNAME, actual.getSurname());
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName(NAME);
        userRequestDto.setSurname(SURNAME);
        userRequestDto.setBirthDate(NEW_BIRTH_DATE);
        userRequestDto.setEmail(UUID.randomUUID() + TEST_EMAIL_DOMAIN);

        mockMvc.perform(put("/api/users/{id}", 999_999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setUserActive_shouldDeactivateUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        String response = mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(Boolean.FALSE, actual.getActive());
    }

    @Test
    void setUserActive_shouldReactivateUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk());

        String response = mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "true"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(Boolean.TRUE, actual.getActive());
    }

    @Test
    void createPaymentCard_shouldCreateCardForUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber(createCardNumber());
        paymentCardCreateDto.setExpirationDate(LocalDate.now().plusYears(2));

        String response = mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardResponseDto actual = objectMapper.readValue(response, PaymentCardResponseDto.class);

        assertEquals(NAME + " " + SURNAME, actual.getHolder());
        assertEquals(userId, actual.getUserId());
    }

    @Test
    void createPaymentCard_shouldReturnBadRequest_whenNumberIsWrongLength() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber("12345");
        paymentCardCreateDto.setExpirationDate(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.number").exists());
    }

    @Test
    void createPaymentCard_shouldReturnBadRequest_whenExpirationDateInPast() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber(createCardNumber());
        paymentCardCreateDto.setExpirationDate(LocalDate.now().minusDays(1));

        mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    void createPaymentCard_shouldReturnConflict_whenNumberAlreadyExists() throws Exception {
        Long firstHolder = createUser(NAME, SURNAME);
        Long secondHolder = createUser(NEW_NAME, NEW_SURNAME);
        String number = createCardNumber();

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber(number);
        paymentCardCreateDto.setExpirationDate(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{id}/cards", firstHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/{id}/cards", secondHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void createPaymentCard_shouldReturnConflict_whenSixthCardForSameUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        for (int i = 0; i < userCardsLimit; i++) {
            createCard(userId);
        }

        PaymentCardCreateDto limitPlusOneDto = new PaymentCardCreateDto();
        limitPlusOneDto.setNumber(createCardNumber());
        limitPlusOneDto.setExpirationDate(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitPlusOneDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnUsersCards() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        createCard(userId);
        createCard(userId);

        mockMvc.perform(get("/api/users/{id}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnEmptyList_whenUserHasNoCards() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        mockMvc.perform(get("/api/users/{id}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/{id}/cards", NON_EXISTENT_ID))
                .andExpect(status().isNotFound());
    }
}
