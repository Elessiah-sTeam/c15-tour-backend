package com.c15tour.backend.controller;

import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.security.JwtUtils;
import com.c15tour.backend.service.RoutingService;
import com.c15tour.backend.service.osrm.OSRMResponse;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class TourEtaIntegrationTest {

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

    @MockitoBean
    private RoutingService routingService;

    private String authHeader;

    @BeforeEach
    void setUp() {
        authHeader = "Bearer " + jwtUtils.generateToken("admin", "ADMIN");

        // Simulate OSRM returning a route with one leg of 3600 seconds
        OSRMResponse.Geometry geometry = new OSRMResponse.Geometry(List.of(), "LineString");
        OSRMResponse.Leg leg = new OSRMResponse.Leg(List.of(), 1000.0, 3600f, "", 0.0);
        OSRMResponse.Route route = new OSRMResponse.Route(geometry, List.of(leg), 1000.0, 3600.0, "routability", 0.0);
        OSRMResponse mockResponse = new OSRMResponse("Ok", List.of(route), List.of());

        when(routingService.calculateRoute(any(), anyBoolean())).thenReturn(mockResponse);
        when(routingService.calculateRoute(any())).thenReturn(mockResponse);
    }

    private TourCreateRequest createTourRequestWithEta(String name, OffsetDateTime departureTime, int breakDuration) {
        TourCreateRequest request = new TourCreateRequest();
        request.setName(name);
        request.setDepartureTime(departureTime);

        Coordinates startCoord = new Coordinates();
        startCoord.setLatitude(47.2184);
        startCoord.setLongitude(-1.5536);

        Waypoints startWp = new Waypoints();
        startWp.setName("Start Point");
        startWp.setCoordinates(startCoord);

        Coordinates endCoord = new Coordinates();
        endCoord.setLatitude(48.8566);
        endCoord.setLongitude(2.3522);

        Waypoints endWp = new Waypoints();
        endWp.setName("End Point");
        endWp.setCoordinates(endCoord);

        SegmentRequest segment = new SegmentRequest();
        segment.setName("Segment 1");
        segment.setBreakDuration(breakDuration);
        segment.setWaypoints(List.of(startWp, endWp));

        request.setSegments(List.of(segment));
        return request;
    }

    @Test
    void createTour_WithDepartureTime_ShouldReturnComputedEtas() throws Exception {
        OffsetDateTime departure = OffsetDateTime.parse("2024-06-01T08:00:00Z");
        TourCreateRequest request = createTourRequestWithEta("ETA Tour", departure, 3600);

        mockMvc.perform(post("/tours")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departureTime", notNullValue()))
                .andExpect(jsonPath("$.segments[0].breakDuration", is(3600)))
                .andExpect(jsonPath("$.segments[0].estimatedDeparture", notNullValue()))
                .andExpect(jsonPath("$.segments[0].waypoints[0].estimatedArrival", notNullValue()))
                .andExpect(jsonPath("$.segments[0].waypoints[1].estimatedArrival", notNullValue()));
    }

    @Test
    void createTour_WithoutDepartureTime_ShouldReturnNullEtas() throws Exception {
        TourCreateRequest request = createTourRequestWithEta("No ETA Tour", null, 0);
        request.setDepartureTime(null);

        mockMvc.perform(post("/tours")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.departureTime").doesNotExist())
                .andExpect(jsonPath("$.segments[0].estimatedDeparture").doesNotExist())
                .andExpect(jsonPath("$.segments[0].waypoints[0].estimatedArrival").doesNotExist())
                .andExpect(jsonPath("$.segments[0].waypoints[1].estimatedArrival").doesNotExist());
    }

    @Test
    void createTour_WithDepartureTime_FirstWaypointShouldMatchDepartureTime() throws Exception {
        OffsetDateTime departure = OffsetDateTime.parse("2024-06-01T08:00:00Z");
        TourCreateRequest request = createTourRequestWithEta("ETA Departure Check", departure, 0);

        mockMvc.perform(post("/tours")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.segments[0].waypoints[0].estimatedArrival", is("2024-06-01T08:00:00Z")));
    }
}
