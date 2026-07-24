package com.innowise.userservice;

import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import com.innowise.userservice.model.dto.PaymentCardCreateDto;
import com.innowise.userservice.model.dto.UserRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest(classes = UserserviceApplication.class)
@AutoConfigureMockMvc
class IntegrationTest {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7")
                    .withExposedPorts(6379);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("userservice")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.data.redis.host",
                redis::getHost
        );
        registry.add(
                "spring.data.redis.port",
                () -> redis.getMappedPort(6379)
        );
    }

    @Test
    void fullUserFlow_shouldCreateUpdateCardAndDeactivate() throws Exception {
        UserRequestDto userRequest = new UserRequestDto();

        userRequest.setName("Ivan");
        userRequest.setSurname("Slesarenko");
        userRequest.setBirthDate(LocalDate.of(2000,1,1));
        userRequest.setEmail("ivan@test.com");
        String userResponse =
                mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(userRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("Ivan"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        Long userId =
                objectMapper.readTree(userResponse)
                        .get("id")
                        .asLong();
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("ivan@test.com"));

        PaymentCardCreateDto cardRequest = new PaymentCardCreateDto();
        cardRequest.setNumber("1111222233334444");
        cardRequest.setExpirationDate(LocalDate.now().plusYears(2));
        mockMvc.perform(post("/api/users/{id}/cards", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder")
                        .value("Ivan Slesarenko"));
        mockMvc.perform(get("/api/users/{id}/cards", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(1));

        UserRequestDto userRequestDto = new UserRequestDto();
        userRequestDto.setName("Alex");
        userRequestDto.setSurname("Petrov");
        userRequestDto.setBirthDate(LocalDate.of(1999,1,1));
        userRequestDto.setEmail("alex@test.com");

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Alex"));

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .param("isActive", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active")
                        .value(false));

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound());
    }
}