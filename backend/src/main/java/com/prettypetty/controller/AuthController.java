package com.prettypetty.controller;

import com.prettypetty.dto.LoginRequest;
import com.prettypetty.dto.ProfileUpdateRequest;
import com.prettypetty.dto.SignupRequest;
import com.prettypetty.dto.UserResponse;
import com.prettypetty.exception.ApiException;
import com.prettypetty.model.User;
import com.prettypetty.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request, HttpServletRequest servletRequest) {
        User user = authService.register(request);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("userId", user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PostMapping("/guest")
    public UserResponse guest(HttpServletRequest servletRequest) {
        User user = authService.createGuest();
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("userId", user.id());
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        User user = authService.authenticate(request);
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("userId", user.id());
        servletRequest.changeSessionId();
        return UserResponse.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.ok().build();
        }
        try {
            return ResponseEntity.ok(UserResponse.from(authService.getUser(authentication.getName())));
        } catch (ApiException exception) {
            if (exception.getStatus() == HttpStatus.UNAUTHORIZED) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            throw exception;
        }
    }

    @PatchMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in to update your profile.");
        }
        return UserResponse.from(authService.updateProfile(authentication.getName(), request));
    }
}
