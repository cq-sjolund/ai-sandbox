package com.consultant.worklog.controller;

import com.consultant.worklog.dto.LoginRequest;
import com.consultant.worklog.dto.LoginResponse;
import com.consultant.worklog.dto.UserDTO;
import com.consultant.worklog.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            log.debug("Login attempt for user: {}", request.getUsername());
            LoginResponse response = authService.login(request);
            log.info("User logged in successfully: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser() {
        log.debug("Fetching current user info");
        UserDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
}
