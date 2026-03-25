package com.medhead.poc.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.medhead.poc.domain.exception.HospitalNotFoundException;
import com.medhead.poc.support.AbstractIntegrationIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class HospitalControllerIT extends AbstractIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listAll_shouldReturnSeededHospitalsWithSpecialtyBedSummary_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/hospitals").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[?(@.name == 'Fred Brooks Hospital')].specialties[?(@.specialtyName == 'Cardiology')].availableBeds")
                        .value(2))
                .andExpect(jsonPath("$[?(@.name == 'Fred Brooks Hospital')].specialties[?(@.specialtyName == 'Immunology')].availableBeds")
                        .value(3));
    }

    @Test
    void findById_shouldReturnFredBrooksWithItsSpecialties_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/hospitals/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Fred Brooks Hospital"))
                .andExpect(jsonPath("$.latitude").value(51.5230))
                .andExpect(jsonPath("$.longitude").value(-0.1300))
                .andExpect(jsonPath("$.address").value("123 Computing Avenue, London WC1E 6BT"))
                .andExpect(jsonPath("$.specialties[?(@.specialtyName == 'Cardiology')].availableBeds").value(2))
                .andExpect(jsonPath("$.specialties[?(@.specialtyName == 'Immunology')].availableBeds").value(3));
    }

    @Test
    void findById_shouldPropagateHospitalNotFoundException_whenHospitalMissing() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/hospitals/9999").with(jwt())))
                .rootCause()
                .isInstanceOf(HospitalNotFoundException.class);
    }

    @Test
    void listAll_shouldReturn401_whenNoBearerTokenProvided() throws Exception {
        mockMvc.perform(get("/api/v1/hospitals"))
                .andExpect(status().isUnauthorized());
    }
}
