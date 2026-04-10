package com.c15tour.backend.controller;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.security.JwtUtils;
import com.c15tour.model.Coordinates;
import com.c15tour.model.SegmentRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.Waypoints;
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

import java.util.ArrayList;
import java.util.List;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
public class TourControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    private String authHeader;

    @BeforeEach
    void setUpAuth() {
        authHeader = "Bearer " + jwtUtils.generateToken("admin", "ADMIN");
    }

    private TourCreateRequest createTourRequest(String name) {
        TourCreateRequest request = new TourCreateRequest();
        request.setName(name);

        // 1. Create Start Waypoint
        Coordinates startCoord = new Coordinates();
        startCoord.setLatitude(47.2184);
        startCoord.setLongitude(-1.5536);

        Waypoints startWp = new Waypoints();
        startWp.setName("Start Point");
        startWp.setCoordinates(startCoord);

        // 2. Create End Waypoint
        Coordinates endCoord = new Coordinates();
        endCoord.setLatitude(48.8566);
        endCoord.setLongitude(2.3522);

        Waypoints endWp = new Waypoints();
        endWp.setName("End Point");
        endWp.setCoordinates(endCoord);

        // 3. Create Segment
        SegmentRequest segment = new SegmentRequest();
        segment.setName("Segment 1");
        segment.setWaypoints(List.of(startWp, endWp));

        // 4. Attach to Tour
        request.setSegments(List.of(segment));

        return request;
    }


    private Tour createTourEntity(String name) {
        Tour tour = new Tour();
        tour.setName(name);
        tour.setTotalDistance(100);
        tour.setTotalDuration(3600);

        // Create Segment
        Segment segment = new Segment();
        segment.setName("Test Segment");
        segment.setTour(tour);
        segment.setDistance(100);
        segment.setDuration(3600);
        segment.setOrderIndex(0);

        // Create Waypoints (Start & End)
        Waypoint wp1 = new Waypoint();
        wp1.setLatitude(10.0);
        wp1.setLongitude(20.0);
        wp1.setOrderIndex(0);
        wp1.setSegment(segment);

        Waypoint wp2 = new Waypoint();
        wp2.setLatitude(30.0);
        wp2.setLongitude(40.0);
        wp2.setOrderIndex(1);
        wp2.setSegment(segment);

        // Link Waypoints to Segment
        segment.setWaypoints(new ArrayList<>(List.of(wp1, wp2)));

        // Link Segment to Tour
        tour.setSegments(new ArrayList<>(List.of(segment)));

        return tourRepository.save(tour);
    }

    @Test
    void createTour_ShouldReturnCreatedTour() throws Exception {
        TourCreateRequest request = createTourRequest("Trip to Nantes");

        mockMvc.perform(post("/tours")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Trip to Nantes")))
                .andExpect(jsonPath("$.segments[0].waypoints[0].coordinates.latitude", is(47.2184)));
    }

    @Test
    void getAllTours_ShouldReturnPaginatedResponse() throws Exception {
        createTourEntity("Tour 1");
        createTourEntity("Tour 2");

        mockMvc.perform(get("/tours")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content[*].name", hasItem("Tour 1")))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    void getAllTours_WithPageAndSize_ShouldReturnRequestedPage() throws Exception {
        for (int i = 1; i <= 5; i++) createTourEntity("Tour " + i);

        mockMvc.perform(get("/tours?page=0&size=2")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    void patchTourDepartureTime_ShouldUpdateDepartureTime() throws Exception {
        Tour savedTour = createTourEntity("Tour to patch");

        String body = "{\"departureTime\": \"2024-06-01T10:00:00Z\"}";

        mockMvc.perform(patch("/tours/" + savedTour.getId())
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departureTime", is("2024-06-01T10:00:00Z")));
    }

    @Test
    void patchTourDepartureTime_WhenNotFound_ShouldReturn404() throws Exception {
        String body = "{\"departureTime\": \"2024-06-01T10:00:00Z\"}";

        mockMvc.perform(patch("/tours/999999")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTourById_ShouldReturnTour() throws Exception {
        Tour savedTour = createTourEntity("Single Tour");

        mockMvc.perform(get("/tours/" + savedTour.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedTour.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Single Tour")));
    }

    @Test
    void getTourById_WhenNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/tours/999999")
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTour_ShouldReturnUpdatedTour() throws Exception {
        Tour savedTour = createTourEntity("Old Name");

        TourCreateRequest updateRequest = createTourRequest("New Name");

        mockMvc.perform(put("/tours/" + savedTour.getId())
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")));
    }

    @Test
    void deleteTour_ShouldRemoveTour() throws Exception {
        Tour savedTour = createTourEntity("To Delete");

        mockMvc.perform(delete("/tours/" + savedTour.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        assertFalse(tourRepository.findById(savedTour.getId()).isPresent());
    }

    @Test
    void deleteTour_WhenNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/tours/999999")
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void createTour_ShouldPersistWaypointNames() throws Exception {
        TourCreateRequest request = createTourRequest("Tour with Named Waypoints");

        mockMvc.perform(post("/tours")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segments[0].waypoints[0].name", is("Start Point")))
                .andExpect(jsonPath("$.segments[0].waypoints[1].name", is("End Point")));
    }

    @Test
    void getTourById_ShouldReturnPersistedWaypointNames() throws Exception {
        Tour tour = new Tour();
        tour.setName("Tour with Waypoint Names");
        tour.setTotalDistance(100);
        tour.setTotalDuration(3600);

        Segment segment = new Segment();
        segment.setName("Test Segment");
        segment.setTour(tour);
        segment.setDistance(100);
        segment.setDuration(3600);
        segment.setOrderIndex(0);

        Waypoint wp1 = new Waypoint();
        wp1.setLatitude(10.0);
        wp1.setLongitude(20.0);
        wp1.setOrderIndex(0);
        wp1.setName("First Waypoint");
        wp1.setSegment(segment);

        Waypoint wp2 = new Waypoint();
        wp2.setLatitude(30.0);
        wp2.setLongitude(40.0);
        wp2.setOrderIndex(1);
        wp2.setName("Second Waypoint");
        wp2.setSegment(segment);

        segment.setWaypoints(new ArrayList<>(List.of(wp1, wp2)));
        tour.setSegments(new ArrayList<>(List.of(segment)));

        Tour savedTour = tourRepository.save(tour);

        mockMvc.perform(get("/tours/" + savedTour.getId())
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segments[0].waypoints[0].name", is("First Waypoint")))
                .andExpect(jsonPath("$.segments[0].waypoints[1].name", is("Second Waypoint")));
    }

}