package com.c15tour.backend.controller;

import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.model.Coordinates;
import com.c15tour.model.TourCreateRequest;
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

        Coordinates start = new Coordinates();
        start.setLatitude(47.2184);
        start.setLongitude(-1.5536);
        request.setStartPoint(start);

        Coordinates end = new Coordinates();
        end.setLatitude(48.8566);
        end.setLongitude(2.3522);
        request.setEndPoint(end);

        request.setWaypoints(List.of());

        return request;
    }

    private Tour createTourEntity(String name) {
        Tour tour = new Tour();
        tour.setName(name);
        tour.setStartLatitude(10.0);
        tour.setStartLongitude(20.0);
        tour.setEndLatitude(30.0);
        tour.setEndLongitude(40.0);
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
