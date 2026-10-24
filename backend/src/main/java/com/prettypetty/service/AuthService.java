package com.prettypetty.service;

import com.prettypetty.dto.LoginRequest;
import com.prettypetty.dto.ProfileUpdateRequest;
import com.prettypetty.dto.SignupRequest;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.User;
import com.prettypetty.repository.ThemeRepository;
import com.prettypetty.repository.UserRepository;
import com.prettypetty.repository.VillainProfileRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VillainProfileRepository villainRepository;
    private final ThemeRepository themeRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VillainProfileRepository villainRepository,
            ThemeRepository themeRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.villainRepository = villainRepository;
        this.themeRepository = themeRepository;
    }

    public User register(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        return userRepository.save(new User(
                UUID.randomUUID().toString(), email, passwordEncoder.encode(request.password()),
                request.displayName().trim(), null, "pink"));
    }

    public User createGuest() {
        String id = UUID.randomUUID().toString();
        return userRepository.save(new User(
                id, "guest-" + id + "@guest.invalid", passwordEncoder.encode(UUID.randomUUID().toString()),
                "Guest", null, "pink"));
    }

    public User authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        return user;
    }

    public User getUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again."));
    }

    public User updateProfile(String userId, ProfileUpdateRequest request) {
        User user = getUser(userId);
        if (!villainRepository.existsById(request.villainId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid villain profile.");
        }
        if (!themeRepository.existsById(request.aestheticId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Choose an available aesthetic.");
        }
        return userRepository.save(new User(
                user.id(), user.email(), user.passwordHash(), user.displayName(),
                request.villainId(), request.aestheticId()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
