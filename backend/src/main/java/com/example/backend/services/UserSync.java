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
        String firstName = resolveFirstName(attributes);
        String lastName = resolveString(attributes, "family_name");

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLastSeen(LocalDateTime.now());

        String sub = token.getSubject();
        if (user.getKeycloakId() == null && sub != null) {
            user.setKeycloakId(sub);
        }

        userRepository.save(user);
        log.info("User synchronized successfully: {}", email);
    }

    private String resolveFirstName(Map<String, Object> attributes) {
        if (attributes.get("given_name") != null) {
            return attributes.get("given_name").toString();
        }
        if (attributes.get("nickname") != null) {
            return attributes.get("nickname").toString();
        }
        if (attributes.get("preferred_username") != null) {
            return attributes.get("preferred_username").toString();
        }
        String email = (String) attributes.get("email");
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf("@"));
        }
        return "Unknown";
    }

    private String resolveString(Map<String, Object> attributes, String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : "";
    }
}