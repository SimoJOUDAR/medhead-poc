package com.medhead.poc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.medhead.poc.application.dto.LoginRequest;
import com.medhead.poc.application.dto.LoginResponse;
import com.medhead.poc.infrastructure.config.SecurityProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final long TTL_MINUTES = 30;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtEncoder jwtEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties(
                new SecurityProperties.Jwt("dev-only-hs256-secret-at-least-32-chars-long", TTL_MINUTES),
                List.of()
        );
        authenticationService = new AuthenticationService(authenticationManager, jwtEncoder, properties);
    }

    @Test
    void login_shouldReturnSignedTokenWithConfiguredTtl_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("demo", "demo");
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("demo", "demo", List.of()));

        Instant fakeIssuedAt = Instant.parse("2026-04-22T10:00:00Z");
        Instant fakeExpiresAt = fakeIssuedAt.plus(TTL_MINUTES, ChronoUnit.MINUTES);
        Jwt fakeJwt = Jwt.withTokenValue("fake.jwt.token")
                .header("alg", "HS256")
                .subject("demo")
                .issuedAt(fakeIssuedAt)
                .expiresAt(fakeExpiresAt)
                .build();
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(fakeJwt);

        Instant before = Instant.now();
        LoginResponse response = authenticationService.login(request);
        Instant after = Instant.now();

        assertThat(response.token()).isEqualTo("fake.jwt.token");
        assertThat(response.issuedAt()).isBetween(before, after);
        assertThat(response.expiresAt())
                .isEqualTo(response.issuedAt().plus(TTL_MINUTES, ChronoUnit.MINUTES));
    }

    @Test
    void login_shouldPropagateBadCredentials_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("demo", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
