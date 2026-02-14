package com.c15tour.backend.controller;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.model.Coordinates;
import com.c15tour.model.SegmentRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.Waypoints;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Trip to Nantes")))
                .andExpect(jsonPath("$.startPoint.latitude", is(47.2184)));
    }

    @Test
    void getAllTours_ShouldReturnList() throws Exception {
        createTourEntity("Tour 1");
        createTourEntity("Tour 2");

        mockMvc.perform(get("/tours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItem("Tour 1")));
    }

    @Test
    void getTourById_ShouldReturnTour() throws Exception {
        Tour savedTour = createTourEntity("Single Tour");

        mockMvc.perform(get("/tours/" + savedTour.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedTour.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Single Tour")));
    }

    @Test
    void getTourById_WhenNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/tours/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTour_ShouldReturnUpdatedTour() throws Exception {
        Tour savedTour = createTourEntity("Old Name");

        TourCreateRequest updateRequest = createTourRequest("New Name");

        mockMvc.perform(put("/tours/" + savedTour.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("New Name")));
    }

    @Test
    void deleteTour_ShouldRemoveTour() throws Exception {
        Tour savedTour = createTourEntity("To Delete");

        mockMvc.perform(delete("/tours/" + savedTour.getId()))
                .andExpect(status().isNoContent());

        assertFalse(tourRepository.findById(savedTour.getId()).isPresent());
    }

    @Test
    void deleteTour_WhenNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/tours/999999"))
                .andExpect(status().isNotFound());
    }
}