package com.innowise.userservice.controller;

import com.innowise.userservice.repository.PaymentCardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WithMockUser(authorities = "ADMIN")
public class IntegrationTestCommons {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private PaymentCardRepository paymentCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${user.cards.limit}")
    protected int userCardsLimit;

    static final GenericContainer<?> redis;
    static final PostgreSQLContainer<?> postgres;

    static {
        redis = new GenericContainer<>("redis:7")
                .withExposedPorts(6379);
        redis.start();

        postgres = new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("userservice")
                .withUsername("postgres")
                .withPassword("postgres");
        postgres.start();
    }

    @AfterEach
    void cleanDatabase() {
        paymentCardRepository.deleteAll();
        userRepository.deleteAll();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    protected Long createUser(String name, String surname) throws Exception {
        String email = UUID.randomUUID() + "@test.com";

        String body = """
                {
                  "name": "%s",
                  "surname": "%s",
                  "birthDate": "2000-01-01",
                  "email": "%s"
                }
                """.formatted(name, surname, email);

        String response = mockMvc.perform(post("/api/v1/users")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    protected Long createCard(Long userId) throws Exception {
        String number = createCardNumber();

        String body = """
                {
                  "number": "%s",
                  "expirationDate": "%s"
                }
                """.formatted(number, LocalDate.now().plusYears(2));

        String response = mockMvc.perform(post("/api/v1/users/{id}/cards", userId)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    protected String createCardNumber() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    protected RequestPostProcessor admin() {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        1L,
                        null,
                        List.of(new SimpleGrantedAuthority("ADMIN"))
                )
        );
    }
}