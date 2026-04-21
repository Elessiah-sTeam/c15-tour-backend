package com.c15tour.backend.controller;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.repository.TourRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class MobileTourControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Tour savedTour;

    @BeforeEach
    void setUp() {
        Tour tour = new Tour();
        tour.setName("Mobile Test Tour");
        tour.setTotalDistance(100);
        tour.setTotalDuration(3600);
        tour.setShareCode("SHARE1");
        tour.setOrganiserCode("ORG001");
        tour.setOrganiserJoined(true);
        tour.setOrganiserSessionToken("valid-session-token");
        tour.setOrganiserTokenExpiresAt(LocalDateTime.now().plusHours(24));

        Segment segment = new Segment();
        segment.setName("Seg");
        segment.setTour(tour);
        segment.setDistance(100);
        segment.setDuration(3600);
        segment.setOrderIndex(0);

        Waypoint wp1 = new Waypoint();
        wp1.setLatitude(47.0);
        wp1.setLongitude(-1.5);
        wp1.setOrderIndex(0);
        wp1.setSegment(segment);

        Waypoint wp2 = new Waypoint();
        wp2.setLatitude(48.0);
        wp2.setLongitude(2.3);
        wp2.setOrderIndex(1);
        wp2.setSegment(segment);

        segment.setWaypoints(new ArrayList<>(List.of(wp1, wp2)));
        tour.setSegments(new ArrayList<>(List.of(segment)));

        savedTour = tourRepository.save(tour);
    }

    @Test
    void updateOrganiserPosition_WithCorrectCode_ShouldReturn204() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("latitude", 47.5, "longitude", -1.2));

        mockMvc.perform(put("/tours/share/ORG001/organiser-position")
                        .header("X-Session-Token", "valid-session-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOrganiserPosition_WithWrongCode_ShouldReturn403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("latitude", 47.5, "longitude", -1.2));

        mockMvc.perform(put("/tours/share/WRONG1/organiser-position")
                        .header("X-Session-Token", "valid-session-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrganiserPosition_WithInvalidToken_ShouldReturn401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("latitude", 47.5, "longitude", -1.2));

        mockMvc.perform(put("/tours/share/SHARE1/organiser-position")
                        .header("X-Session-Token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
