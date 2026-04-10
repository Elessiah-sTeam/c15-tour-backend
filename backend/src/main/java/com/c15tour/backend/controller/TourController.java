package com.c15tour.backend.controller;

import com.c15tour.api.ToursApi;
import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.mapper.TourMapper;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.service.RoutingService;
import com.c15tour.backend.service.ShareCodeService;
import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import com.c15tour.model.PatchDepartureTimeRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TourController implements ToursApi {

    private final TourRepository tourRepository;
    private final TourMapper tourMapper;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private final ShareCodeService shareCodeService;

    public TourController(TourRepository tourRepository, TourMapper tourMapper, RoutingService routingService, ObjectMapper objectMapper, ShareCodeService shareCodeService) {
        this.tourRepository = tourRepository;
        this.tourMapper = tourMapper;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
        this.shareCodeService = shareCodeService;
    }

    @Override
    public ResponseEntity<TourResponse> createTour(TourCreateRequest tourCreateRequest) {
        Tour tourEntity = tourMapper.toEntity(tourCreateRequest);

        calculateTourRoutes(tourEntity);

        tourEntity.setShareCode(shareCodeService.generateUniqueShareCode());
        tourEntity.setOrganiserCode(shareCodeService.generateUniqueOrganiserCode());

        Tour savedTour = tourRepository.save(tourEntity);
        return new ResponseEntity<>(tourMapper.toResponse(savedTour), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteTour(Long id) {
        if (tourRepository.existsById(id)) {
            tourRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<TourResponse>> getAllTours() {
        List<TourResponse> tourResponses = tourRepository.findAll().stream()
                .map(tourMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tourResponses);
    }

    @Override
    public ResponseEntity<TourResponse> getTourById(Long id) {
        return tourRepository.findById(id)
                .map(tourMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> updateTour(Long id, TourCreateRequest tourCreateRequest) {
        return tourRepository.findById(id)
                .map(existingTour -> {
                    tourMapper.updateEntity(existingTour, tourCreateRequest);

                    calculateTourRoutes(existingTour);

                    Tour savedTour = tourRepository.save(existingTour);
                    return ResponseEntity.ok(tourMapper.toResponse(savedTour));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> patchTourDepartureTime(Long id, PatchDepartureTimeRequest request) {
        return tourRepository.findById(id)
                .map(tour -> {
                    tour.setDepartureTime(request.getDepartureTime().toLocalDateTime());
                    recalculateEtas(tour);
                    tourRepository.save(tour);
                    return ResponseEntity.ok(tourMapper.toResponse(tour));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> getTourByShareCode(String code) {
        return tourRepository.findByShareCode(code)
                .map(tourMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private void calculateTourRoutes(Tour tour) {
        if (tour.getSegments() == null) return;

        int totalDist = 0;
        int totalDur = 0;
        java.time.LocalDateTime runningTime = tour.getDepartureTime();

        for (Segment segment : tour.getSegments()) {
            List<Waypoint> waypoints = segment.getWaypoints();
            if (waypoints == null || waypoints.size() < 2) {
                continue; // Pas assez de waypoints pour calculer une route
            }

            List<Waypoint> sortedWaypoints = waypoints.stream()
                    .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                    .collect(Collectors.toList());

            List<Coordinates> coords = sortedWaypoints.stream()
                    .map(wp -> {
                        Coordinates c = new Coordinates();
                        c.setLatitude(wp.getLatitude());
                        c.setLongitude(wp.getLongitude());
                        return c;
                    })
                    .collect(Collectors.toList());

            // On appelle OSRM
            OSRMResponse response = routingService.calculateRoute(coords, true);

            // On met à jour le segment
            if (response != null && response.routes() != null && !response.routes().isEmpty()) {
                var route = response.routes().getFirst();

                int dist = route.distance() != null ? (int) Math.round(route.distance()) : 0;
                int dur = route.duration() != null ? (int) Math.round(route.duration()) : 0;

                segment.setDistance(dist);
                segment.setDuration(dur);

                totalDist += dist;
                totalDur += dur;

                // Store per-leg durations for ETA computation
                List<OSRMResponse.Leg> legs = route.legs();

                try {
                    String geometryJson = objectMapper.writeValueAsString(route.geometry());
                    segment.setGeometry(geometryJson);

                    if (legs != null) {
                        List<OSRMResponse.Step> allSteps = legs.stream()
                                .flatMap(leg -> leg.steps() != null ? leg.steps().stream() : java.util.stream.Stream.empty())
                                .collect(Collectors.toList());

                        String stepsJson = objectMapper.writeValueAsString(allSteps);
                        segment.setSteps(stepsJson);
                    }
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                // Compute per-waypoint ETAs using OSRM leg durations
                if (tour.getDepartureTime() != null && legs != null) {
                    for (int i = 0; i < sortedWaypoints.size(); i++) {
                        sortedWaypoints.get(i).setEstimatedArrival(runningTime);
                        if (i < legs.size() && legs.get(i).duration() != null) {
                            runningTime = runningTime.plusSeconds(Math.round(legs.get(i).duration()));
                        }
                    }
                    int breakSecs = segment.getBreakDuration() != null ? segment.getBreakDuration() : 0;
                    segment.setEstimatedDeparture(runningTime.plusSeconds(breakSecs));
                    runningTime = segment.getEstimatedDeparture();
                }
            }
        }

        tour.setTotalDistance(totalDist);
        tour.setTotalDuration(totalDur);
    }

    /**
     * Recomputes per-waypoint ETAs and per-segment estimatedDeparture from stored
     * segment durations — no OSRM call. Waypoint ETAs are distributed linearly
     * across each segment's total duration.
     */
    private void recalculateEtas(Tour tour) {
        if (tour.getDepartureTime() == null || tour.getSegments() == null) return;

        LocalDateTime runningTime = tour.getDepartureTime();

        List<Segment> sortedSegments = tour.getSegments().stream()
                .sorted(Comparator.comparingInt(Segment::getOrderIndex))
                .collect(Collectors.toList());

        for (Segment segment : sortedSegments) {
            List<Waypoint> waypoints = segment.getWaypoints();
            if (waypoints == null || waypoints.isEmpty()) continue;

            List<Waypoint> sorted = waypoints.stream()
                    .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                    .collect(Collectors.toList());

            int n = sorted.size();
            int segDur = segment.getDuration() != null ? segment.getDuration() : 0;

            for (int i = 0; i < n; i++) {
                long offset = n > 1 ? (long) i * segDur / (n - 1) : 0;
                sorted.get(i).setEstimatedArrival(runningTime.plusSeconds(offset));
            }

            runningTime = runningTime.plusSeconds(segDur);
            int breakSecs = segment.getBreakDuration() != null ? segment.getBreakDuration() : 0;
            segment.setEstimatedDeparture(runningTime.plusSeconds(breakSecs));
            runningTime = segment.getEstimatedDeparture();
        }
    }
}