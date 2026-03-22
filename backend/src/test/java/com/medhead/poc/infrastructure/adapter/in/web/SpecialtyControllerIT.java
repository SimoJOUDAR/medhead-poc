package com.medhead.poc.infrastructure.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SpecialtyControllerIT extends AbstractIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listAll_shouldReturnSeededNhsCatalogue_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/specialties").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(80))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].group.id").isNumber())
                .andExpect(jsonPath("$[0].group.name").isString());
    }

    @Test
    void listAll_shouldReturn401_whenNoBearerTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isUnauthorized());
    }
}
