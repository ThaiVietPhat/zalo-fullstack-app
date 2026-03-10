package com.example.backend.services;

import com.example.backend.Entities.User;
import com.example.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSync {

    private final UserRepository userRepository;

    @Transactional
    public void syncUserFromToken(Jwt token) {
        Map<String, Object> attributes = token.getClaims();
        String email = (String) attributes.get("email");

        if (email == null) {
            log.warn("Token does not contain email, skipping synchronization");
            return;
        }

        log.info("Synchronizing user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElse(new User());

        user.setEmail(email);
        user.setFirstName(attributes.getOrDefault("given_name", attributes.get("nickname")).toString());
        user.setLastName((String) attributes.get("family_name"));
        user.setLastSeen(LocalDateTime.now());

        userRepository.save(user);
    }
}