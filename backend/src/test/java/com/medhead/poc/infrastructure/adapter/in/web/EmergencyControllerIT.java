package com.medhead.poc.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.RouteInfo;
import com.medhead.poc.domain.port.out.DistanceCalculator;
import com.medhead.poc.infrastructure.adapter.in.web.EmergencyControllerIT.RecordingBedReservationListenerConfiguration.RecordingBedReservationListener;
import com.medhead.poc.support.AbstractIntegrationIT;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@Import(EmergencyControllerIT.RecordingBedReservationListenerConfiguration.class)
class EmergencyControllerIT extends AbstractIntegrationIT {

    private static final long CARDIOLOGY_ID = 21L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingBedReservationListener eventListener;

    @MockitoBean
    private DistanceCalculator distanceCalculator;

    @BeforeEach
    void stubDistanceCalculator() {
        when(distanceCalculator.calculate(any(), any())).thenAnswer(invocation -> {
            var destination = invocation.getArgument(1, com.medhead.poc.domain.model.GpsCoordinates.class);
            if (destination.latitude() > 53.0) {
                return new RouteInfo(260.0, 200.0);
            }
            if (destination.latitude() < 52.0 && destination.longitude() < -2.0) {
                return new RouteInfo(180.0, 150.0);
            }
            return new RouteInfo(0.2, 1.0);
        });
        eventListener.reset();
    }

    @Test
    void recommend_shouldReserveBedAndPublishEvent_whenAuthenticated() throws Exception {
        String body = """
                {
                    "specialtyId": %d,
                    "latitude": 51.5230,
                    "longitude": -0.1310
                }
                """.formatted(CARDIOLOGY_ID);

        mockMvc.perform(post("/api/v1/emergency/recommend")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospital.name").value("Fred Brooks Hospital"))
                .andExpect(jsonPath("$.hospital.latitude").value(51.5230))
                .andExpect(jsonPath("$.hospital.longitude").value(-0.1300))
                .andExpect(jsonPath("$.hospital.availableBeds").value(1))
                .andExpect(jsonPath("$.hospital.distanceKm").value(0.2))
                .andExpect(jsonPath("$.hospital.estimatedTravelTimeMinutes").value(1.0))
                .andExpect(jsonPath("$.specialty.name").value("Cardiology"))
                .andExpect(jsonPath("$.specialty.group").value("General medicine group"))
                .andExpect(jsonPath("$.bedReserved").value(true))
                .andExpect(jsonPath("$.fallback").value(false))
                .andExpect(jsonPath("$.timestamp").exists());

        Integer bedsAfter = jdbcTemplate.queryForObject("""
                SELECT available_beds FROM hospital_specialties
                 WHERE hospital_id = 1 AND specialty_id = ?
                """, Integer.class, CARDIOLOGY_ID);
        assertThat(bedsAfter).isEqualTo(1);

        List<BedReservationEvent> events = eventListener.captured();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).hospitalId()).isEqualTo(1L);
        assertThat(events.get(0).hospitalName()).isEqualTo("Fred Brooks Hospital");
        assertThat(events.get(0).specialtyId()).isEqualTo(CARDIOLOGY_ID);
        assertThat(events.get(0).specialtyName()).isEqualTo("Cardiology");
        assertThat(events.get(0).remainingBeds()).isEqualTo(1);
    }

    @Test
    void recommend_shouldReturn401_whenNoBearerTokenProvided() throws Exception {
        String body = """
                {
                    "specialtyId": %d,
                    "latitude": 51.5230,
                    "longitude": -0.1310
                }
                """.formatted(CARDIOLOGY_ID);

        mockMvc.perform(post("/api/v1/emergency/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class RecordingBedReservationListenerConfiguration {

        @Bean
        RecordingBedReservationListener recordingBedReservationListener() {
            return new RecordingBedReservationListener();
        }

        static class RecordingBedReservationListener {
            private final List<BedReservationEvent> events = new ArrayList<>();

            @EventListener
            public void on(BedReservationEvent event) {
                events.add(event);
            }

            List<BedReservationEvent> captured() {
                return List.copyOf(events);
            }

            void reset() {
                events.clear();
            }
        }
    }
}
