package com.c15tour.backend.controller;

import com.c15tour.api.TourCalculationApi;
import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.mapper.WaypointMapper;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.service.RoutingService;
import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import com.c15tour.model.RouteToStartResponse;
import com.c15tour.model.UserPositionRequest;
import com.c15tour.model.Waypoints;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class TourCalculationController implements TourCalculationApi {

    private final TourRepository tourRepository;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private final WaypointMapper waypointMapper;

    public TourCalculationController(TourRepository tourRepository, RoutingService routingService, ObjectMapper objectMapper, WaypointMapper waypointMapper) {
        this.tourRepository = tourRepository;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
        this.waypointMapper = waypointMapper;
    }

    @Override
    public ResponseEntity<RouteToStartResponse> getRouteToStart(Long id, @Valid UserPositionRequest userPositionRequest) {
        Tour tour = tourRepository.findById(id).orElse(null);
        if (tour == null) {
            return ResponseEntity.notFound().build();
        }

        Waypoint startWaypoint = getFirstWaypoint(tour);
        if (startWaypoint == null) {
            return ResponseEntity.badRequest().build();
        }

        Coordinates userCoords = userPositionRequest.getCoordinates();

        Waypoints startPointDto = waypointMapper.toDto(startWaypoint);
        Coordinates startCoords = startPointDto.getCoordinates();
        OSRMResponse osrmResponse = routingService.calculateRoute(List.of(userCoords, startCoords), true);

        if (osrmResponse == null || osrmResponse.routes().isEmpty() || osrmResponse.routes() == null) {
            return ResponseEntity.badRequest().build();
        }

        var route = osrmResponse.routes().getFirst();
        RouteToStartResponse response = new RouteToStartResponse();

        response.setDistance(route.distance() != null ? (long) Math.round(route.distance()) : 0L);
        response.setDuration(route.duration() != null ? (long) Math.round(route.duration()) : 0L);

        try {
            response.setGeometry(objectMapper.writeValueAsString(route.geometry()));

            if (route.legs() != null) {
                List<OSRMResponse.Step> allSteps = route.legs().stream()
                        .filter(leg -> leg.steps() != null)
                        .flatMap(leg -> leg.steps().stream())
                        .toList();
                response.setSteps(objectMapper.writeValueAsString(allSteps));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.setStartPoint(startPointDto);

        return ResponseEntity.ok(response);
    }

    private Waypoint getFirstWaypoint(Tour tour) {
        if (tour.getSegments() == null || tour.getSegments().isEmpty()) {
            return null;
        }

        Segment firstSegment = tour.getSegments().stream()
                .min(Comparator.comparingInt(Segment::getOrderIndex))
                .orElse(null);

        if (firstSegment == null || firstSegment.getWaypoints() == null
                || firstSegment.getWaypoints().isEmpty()) {
            return null;
        }

        return firstSegment.getWaypoints().stream()
                .min(Comparator.comparingInt(Waypoint::getOrderIndex))
                .orElse(null);
    }
}
