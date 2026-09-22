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

@SpringBootTest(
        classes = UserserviceApplication.class,
        properties = "jwt.secret=jwt-secret-for-test-JzdWIiOiI1Iiwicm9sZSI6IlVTRVIiLCJpYXQiOjE3ODU3NTM1MjAsImV4cC"
)
@AutoConfigureMockMvc(addFilters = false)
class UserIntegrationTest extends IntegrationTestCommons {

    private static final String NAME = "Ivan";
    private static final String SURNAME = "Slesarenko";
    private static final String NEW_NAME = "Jan";
    private static final String NEW_SURNAME = "Slesarensky";
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate NEW_BIRTH_DATE = LocalDate.of(1999, 1, 1);
    private static final Long NON_EXISTENT_ID = 999_999_999L;
    private static final String TEST_EMAIL_DOMAIN = "@test.com";
    private static final String URI = "/api/v1/users";
    private static final String URI_W_ID = "/api/v1/users/{id}";
    private static final String URI_W_ID_STATUS = "/api/v1/users/{id}/status";
    private static final String URI_W_ID_CARDS = "/api/v1/users/{id}/cards";

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name(NAME)
                .surname(SURNAME)
                .birthDate(BIRTH_DATE)
                .email(UUID.randomUUID() + TEST_EMAIL_DOMAIN)
                .build();

        String response = mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(NAME, actual.name());
        assertEquals(SURNAME, actual.surname());
        assertTrue(actual.active());
        assertNotNull(actual.id());
        assertTrue(actual.paymentCards().isEmpty());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name("")
                .surname(SURNAME)
                .birthDate(LocalDate.of(2000, 1, 1))
                .email(UUID.randomUUID() + TEST_EMAIL_DOMAIN)
                .build();

        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name(NAME)
                .surname(SURNAME)
                .birthDate(LocalDate.of(2000, 1, 1))
                .email(UUID.randomUUID().toString())
                .build();

        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenBirthDateInFuture() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name(NAME)
                .surname(SURNAME)
                .birthDate(LocalDate.now().plusDays(1))
                .email(UUID.randomUUID() + TEST_EMAIL_DOMAIN)
                .build();

        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.birthDate").exists());
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        String response = mockMvc.perform(get(URI_W_ID, userId).with(admin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(userId, actual.id());
        assertEquals(NAME, actual.name());
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get(URI_W_ID, NON_EXISTENT_ID).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_shouldReturnBadRequest_whenUserIsNotActive() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        mockMvc.perform(patch(URI_W_ID_STATUS, userId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get(URI_W_ID, userId).with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_shouldFilterByNameAndSurname() throws Exception {
        Long userId = createUser(NEW_NAME, NEW_SURNAME);

        mockMvc.perform(get(URI)
                        .param("name", NEW_NAME)
                        .param("surname", NEW_SURNAME)
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(userId));
    }

    @Test
    void getAllUsers_shouldReturnEmptyPage_whenNoMatch() throws Exception {
        mockMvc.perform(get(URI)
                        .param("name", "definitely-not-existing-" + UUID.randomUUID())
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void updateUser_shouldUpdateFields() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name(NEW_NAME)
                .surname(NEW_SURNAME)
                .birthDate(NEW_BIRTH_DATE)
                .email(UUID.randomUUID() + TEST_EMAIL_DOMAIN)
                .build();

        String response = mockMvc.perform(put(URI_W_ID, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(NEW_NAME, actual.name());
        assertEquals(NEW_SURNAME, actual.surname());
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        UserRequestDto userRequestDto = UserRequestDto.builder()
                .name(NAME)
                .surname(SURNAME)
                .birthDate(NEW_BIRTH_DATE)
                .email(UUID.randomUUID() + TEST_EMAIL_DOMAIN)
                .build();

        mockMvc.perform(put(URI_W_ID, NON_EXISTENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))
                        .with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void setUserActive_shouldDeactivateUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        String response = mockMvc.perform(patch(URI_W_ID_STATUS, userId)
                        .param("isActive", "false")
                        .with(admin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(Boolean.FALSE, actual.active());
    }

    @Test
    void setUserActive_shouldReactivateUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        mockMvc.perform(patch(URI_W_ID_STATUS, userId)
                        .param("isActive", "false")
                        .with(admin()))
                .andExpect(status().isOk());

        String response = mockMvc.perform(patch(URI_W_ID_STATUS, userId)
                        .param("isActive", "true")
                        .with(admin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UserResponseDto actual = objectMapper.readValue(response, UserResponseDto.class);

        assertEquals(Boolean.TRUE, actual.active());
    }

    @Test
    void createPaymentCard_shouldCreateCardForUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = PaymentCardCreateDto.builder()
                .number(createCardNumber())
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        String response = mockMvc.perform(post(URI_W_ID_CARDS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto))
                        .with(admin()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        PaymentCardResponseDto actual = objectMapper.readValue(response, PaymentCardResponseDto.class);

        assertEquals(NAME + " " + SURNAME, actual.holder());
        assertEquals(userId, actual.userId());
    }

    @Test
    void createPaymentCard_shouldReturnBadRequest_whenNumberIsWrongLength() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = PaymentCardCreateDto.builder()
                .number("12345")
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post(URI_W_ID_CARDS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.number").exists());
    }

    @Test
    void createPaymentCard_shouldReturnBadRequest_whenExpirationDateInPast() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        PaymentCardCreateDto paymentCardCreateDto = PaymentCardCreateDto.builder()
                .number(createCardNumber())
                .expirationDate(LocalDate.now().minusDays(1))
                .build();

        mockMvc.perform(post(URI_W_ID_CARDS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto))
                        .with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expirationDate").exists());
    }

    @Test
    void createPaymentCard_shouldReturnConflict_whenNumberAlreadyExists() throws Exception {
        Long firstHolder = createUser(NAME, SURNAME);
        Long secondHolder = createUser(NEW_NAME, NEW_SURNAME);
        String number = createCardNumber();

        PaymentCardCreateDto paymentCardCreateDto = PaymentCardCreateDto.builder()
                .number(number)
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post(URI_W_ID_CARDS, firstHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto))
                        .with(admin()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URI_W_ID_CARDS, secondHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto))
                        .with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void createPaymentCard_shouldReturnConflict_whenSixthCardForSameUser() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        for (int i = 0; i < userCardsLimit; i++) {
            createCard(userId);
        }

        PaymentCardCreateDto limitPlusOneDto = PaymentCardCreateDto.builder()
                .number(createCardNumber())
                .expirationDate(LocalDate.now().plusYears(2))
                .build();

        mockMvc.perform(post(URI_W_ID_CARDS, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(limitPlusOneDto))
                        .with(admin()))
                .andExpect(status().isConflict());
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnUsersCards() throws Exception {
        Long userId = createUser(NAME, SURNAME);
        createCard(userId);
        createCard(userId);

        mockMvc.perform(get(URI_W_ID_CARDS, userId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnEmptyList_whenUserHasNoCards() throws Exception {
        Long userId = createUser(NAME, SURNAME);

        mockMvc.perform(get(URI_W_ID_CARDS, userId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get(URI_W_ID_CARDS, NON_EXISTENT_ID).with(admin()))
                .andExpect(status().isNotFound());
    }
}
