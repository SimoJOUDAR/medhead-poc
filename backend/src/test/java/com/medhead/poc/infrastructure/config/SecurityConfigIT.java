package com.medhead.poc.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void actuatorHealth_shouldBeReachableAnonymously() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_shouldBeReachableAnonymously() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void apiDocs_shouldBeReachableAnonymously() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void ping_shouldBeReachableAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpoint_shouldBeReachableAnonymously() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "demo", "password": "demo" }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void arbitraryApiPath_shouldRequireAuthentication_whenNoTokenIsProvided() throws Exception {
        mockMvc.perform(get("/api/v1/anything-protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void arbitraryApiPath_shouldBeAcceptedWithValidBearerToken_andReturn404ForUnknownRoute() throws Exception {
        String token = obtainDemoToken();

        mockMvc.perform(get("/api/v1/anything-protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void arbitraryApiPath_shouldReturn401_whenBearerTokenIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/anything-protected")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private String obtainDemoToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "username": "demo", "password": "demo" }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
