package com.innowise.userservice.controller;

import com.innowise.userservice.UserserviceApplication;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.UserRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(classes = UserserviceApplication.class)
@AutoConfigureMockMvc
class UserIntegrationTest extends IntegrationTestCommons {

    @Test
    void createUser_shouldReturnCreatedUser() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Ivan");
        userRequestDto.setSurname("Slesarenko");
        userRequestDto.setBirthDate(LocalDate.of(2000, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID() + "@test.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.surname").value("Slesarenko"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.paymentCards").isEmpty());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenNameIsBlank() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("");
        userRequestDto.setSurname("Slesarenko");
        userRequestDto.setBirthDate(LocalDate.of(2000, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID() + "ivan@test.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Ivan");
        userRequestDto.setSurname("Slesarenko");
        userRequestDto.setBirthDate(LocalDate.of(2000, 1, 1));
        userRequestDto.setEmail("invalid-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void createUser_shouldReturnBadRequest_whenBirthDateInFuture() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Ivan");
        userRequestDto.setSurname("Slesarenko");
        userRequestDto.setBirthDate(LocalDate.now().plusDays(1));
        userRequestDto.setEmail(UUID.randomUUID() + "ivan@test.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.birthDate").exists());
    }

    @Test
    void getUserById_shouldReturnUser_whenExists() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Ivan"));
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserById_shouldReturnBadRequest_whenUserIsNotActive() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_shouldFilterByNameAndSurname() throws Exception {
        String name = "Zoro-" + UUID.randomUUID();
        String surname = "Roronoa-" + UUID.randomUUID();
        Long userId = createUser(name, surname);

        mockMvc.perform(get("/api/users")
                        .param("name", name)
                        .param("surname", surname))
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
        Long userId = createUser("Ivan", "Slesarenko");

        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Jan");
        userRequestDto.setSurname("Slesarensky");
        userRequestDto.setBirthDate(LocalDate.of(1999, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID() + "@test.com");

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan"))
                .andExpect(jsonPath("$.surname").value("Slesarensky"));
    }

    @Test
    void updateUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Alex");
        userRequestDto.setSurname("Petrov");
        userRequestDto.setBirthDate(LocalDate.of(1999, 1, 1));
        userRequestDto.setEmail(UUID.randomUUID() + "@test.com");

        mockMvc.perform(put("/api/users/{id}", 999_999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void setUserActive_shouldDeactivateUser() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void setUserActive_shouldReactivateUser() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "false"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/{id}/status", userId).param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createPaymentCard_shouldCreateCardForUser() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber(createCardNumber());
        paymentCardCreateDto.setExpirationDate(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Ivan Slesarenko"))
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void createPaymentCard_shouldReturnBadRequest_whenNumberIsWrongLength() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

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
        Long userId = createUser("Ivan", "Slesarenko");

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
        Long firstHolder = createUser("Ivan", "Slesarenko");
        Long secondHolder = createUser("Alex", "Petrov");
        String number = createCardNumber();

        PaymentCardCreateDto paymentCardCreateDto = new PaymentCardCreateDto();
        paymentCardCreateDto.setNumber(number);
        paymentCardCreateDto.setExpirationDate(LocalDate.now().plusYears(2));

        mockMvc.perform(post("/api/users/{id}/cards", firstHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/{id}/cards", secondHolder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentCardCreateDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void createPaymentCard_shouldReturnConflict_whenSixthCardForSameUser() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");
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
        Long userId = createUser("Ivan", "Slesarenko");
        createCard(userId);
        createCard(userId);

        mockMvc.perform(get("/api/users/{id}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnEmptyList_whenUserHasNoCards() throws Exception {
        Long userId = createUser("Ivan", "Slesarenko");

        mockMvc.perform(get("/api/users/{id}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllPaymentCardsByUserId_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/{id}/cards", 999_999_999L))
                .andExpect(status().isNotFound());
    }
}
