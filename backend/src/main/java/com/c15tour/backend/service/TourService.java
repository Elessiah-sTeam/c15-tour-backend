package com.c15tour.backend.service;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.mapper.TourMapper;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import com.c15tour.model.PatchDepartureTimeRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourPageResponse;
import com.c15tour.model.TourResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TourService {

    private static final Logger log = LoggerFactory.getLogger(TourService.class);

    private final TourRepository tourRepository;
    private final TourMapper tourMapper;
    private final RoutingService routingService;
    private final ShareCodeService shareCodeService;
    private final ObjectMapper objectMapper;

    public TourService(TourRepository tourRepository, TourMapper tourMapper,
                       RoutingService routingService, ShareCodeService shareCodeService,
                       ObjectMapper objectMapper) {
        this.tourRepository = tourRepository;
        this.tourMapper = tourMapper;
        this.routingService = routingService;
        this.shareCodeService = shareCodeService;
        this.objectMapper = objectMapper;
    }

    public TourResponse create(TourCreateRequest request) {
        Tour tour = tourMapper.toEntity(request);
        calculateTourRoutes(tour);
        tour.setShareCode(shareCodeService.generateUniqueShareCode());
        tour.setOrganiserCode(shareCodeService.generateUniqueOrganiserCode());
        return tourMapper.toResponse(tourRepository.save(tour));
    }

    public Optional<TourResponse> update(Long id, TourCreateRequest request) {
        return tourRepository.findById(id).map(existing -> {
            tourMapper.updateEntity(existing, request);
            calculateTourRoutes(existing);
            return tourMapper.toResponse(tourRepository.save(existing));
        });
    }

    public Optional<TourResponse> patchDepartureTime(Long id, PatchDepartureTimeRequest request) {
        return tourRepository.findById(id).map(tour -> {
            tour.setDepartureTime(request.getDepartureTime().toLocalDateTime());
            recalculateEtas(tour);
            return tourMapper.toResponse(tourRepository.save(tour));
        });
    }

    public boolean delete(Long id) {
        if (!tourRepository.existsById(id)) return false;
        tourRepository.deleteById(id);
        return true;
    }

    public TourPageResponse getAll(int page, int size) {
        Page<Tour> tourPage = tourRepository.findAll(PageRequest.of(page, size));
        TourPageResponse response = new TourPageResponse();
        response.setContent(tourPage.getContent().stream().map(tourMapper::toResponse).collect(Collectors.toList()));
        response.setTotalElements(tourPage.getTotalElements());
        response.setTotalPages(tourPage.getTotalPages());
        response.setPage(tourPage.getNumber());
        return response;
    }

    public Optional<TourResponse> getById(Long id) {
        return tourRepository.findById(id).map(tourMapper::toResponse);
    }

    public Optional<TourResponse> getByShareCode(String code) {
        Optional<Tour> byShareCode = tourRepository.findByShareCode(code);
        if (byShareCode.isPresent()) {
            TourResponse response = tourMapper.toResponse(byShareCode.get());
            response.setOrganiserCode(null);
            response.setRole(TourResponse.RoleEnum.PARTICIPANT);
            return Optional.of(response);
        }
        return tourRepository.findByOrganiserCode(code).map(tour -> {
            TourResponse response = tourMapper.toResponse(tour);
            response.setOrganiserCode(null);
            response.setRole(TourResponse.RoleEnum.ORGANISER);
            return response;
        });
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void calculateTourRoutes(Tour tour) {
        if (tour.getSegments() == null) return;

        int totalDist = 0;
        int totalDur = 0;
        LocalDateTime runningTime = tour.getDepartureTime();

        for (Segment segment : tour.getSegments()) {
            List<Waypoint> waypoints = segment.getWaypoints();
            if (waypoints == null || waypoints.size() < 2) continue;

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

            OSRMResponse response = routingService.calculateRoute(coords, true);

            if (response != null && response.routes() != null && !response.routes().isEmpty()) {
                var route = response.routes().getFirst();

                int dist = route.distance() != null ? (int) Math.round(route.distance()) : 0;
                int dur = route.duration() != null ? (int) Math.round(route.duration()) : 0;

                segment.setDistance(dist);
                segment.setDuration(dur);

                totalDist += dist;
                totalDur += dur;

                List<OSRMResponse.Leg> legs = route.legs();

                try {
                    segment.setGeometry(objectMapper.writeValueAsString(route.geometry()));
                    if (legs != null) {
                        List<OSRMResponse.Step> allSteps = legs.stream()
                                .flatMap(leg -> leg.steps() != null ? leg.steps().stream() : java.util.stream.Stream.empty())
                                .collect(Collectors.toList());
                        segment.setSteps(objectMapper.writeValueAsString(allSteps));
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize OSRM geometry/steps for segment '{}'", segment.getName(), e);
                }

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
