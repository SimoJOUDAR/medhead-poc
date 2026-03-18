package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.application.dto.LoginRequest;
import com.medhead.poc.application.dto.LoginResponse;
import com.medhead.poc.application.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credential-exchange endpoint. Accepts a username/password pair and returns a
 * signed JWT used to authenticate subsequent calls against the rest of the
 * {@code /api/v1/**} surface.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }
}
