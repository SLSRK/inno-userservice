package com.innowise.userservice.security;

import com.innowise.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    public boolean isUser(String email, Long id) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId().equals(id))
                .orElse(false);
    }
}
