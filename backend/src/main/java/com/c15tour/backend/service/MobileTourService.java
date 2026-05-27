package com.c15tour.backend.service;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.backend.service.osrm.OSRMTableResponse;
import com.c15tour.model.Coordinates;
import com.c15tour.model.JoinResponse;
import com.c15tour.model.OrganiserPositionRequest;
import com.c15tour.model.RouteToStartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MobileTourService {

    private static final int REDIRECT_CANDIDATE_COUNT = 5;

    private final TourRepository tourRepository;
    private final RoutingService routingService;
    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public MobileTourService(TourRepository tourRepository,
                             RoutingService routingService,
                             ObjectMapper objectMapper) {
        this.tourRepository = tourRepository;
        this.routingService = routingService;
        this.objectMapper = objectMapper;
    }

    public JoinResponse join(String code) {
        Tour tour = tourRepository.findByOrganiserCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid organiser code"));

        String sessionToken = UUID.randomUUID().toString();
        tour.setOrganiserJoined(true);
        tour.setOrganiserSessionToken(sessionToken);
        tour.setOrganiserTokenExpiresAt(LocalDateTime.now().plusHours(24));
        tourRepository.save(tour);

        JoinResponse response = new JoinResponse();
        response.setRole(JoinResponse.RoleEnum.ORGANISER);
        response.setSessionToken(sessionToken);
        return response;
    }

    public void updateOrganiserPosition(String code, String sessionToken, OrganiserPositionRequest body) {
        Tour tour = tourRepository.findByOrganiserSessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session token"));
        if (tour.getOrganiserTokenExpiresAt() == null || LocalDateTime.now().isAfter(tour.getOrganiserTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session token expired");
        }
        if (!code.equals(tour.getOrganiserCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session token does not match this tour");
        }
        tour.setOrganiserLat(body.getLatitude());
        tour.setOrganiserLng(body.getLongitude());
        tourRepository.save(tour);
        pushToEmitters(tour.getId(), body.getLatitude(), body.getLongitude());
    }

    public SseEmitter streamOrganiserPosition(String shareCode) {
        Tour tour = tourRepository.findByShareCode(shareCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(tour.getId(), k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tour.getId(), emitter));
        emitter.onTimeout(() -> removeEmitter(tour.getId(), emitter));

        // Send current position immediately if already available
        if (tour.getOrganiserLat() != null && tour.getOrganiserLng() != null) {
            try {
                emitter.send(SseEmitter.event().data(
                        Map.of("latitude", tour.getOrganiserLat(), "longitude", tour.getOrganiserLng())
                ));
            } catch (IOException e) {
                emitter.complete();
            }
        }

        return emitter;
    }

    public void pushAudioMessageEvent(Long tourId, long messageId, String url, LocalDateTime createdAt) {
        List<SseEmitter> tourEmitters = emitters.get(tourId);
        if (tourEmitters == null) return;

        Map<String, Object> event = Map.of(
                "type", "AUDIO_MESSAGE",
                "id", messageId,
                "url", url,
                "createdAt", createdAt.toString()
        );

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : tourEmitters) {
            try {
                emitter.send(SseEmitter.event().data(event));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        tourEmitters.removeAll(dead);
    }

    private void pushToEmitters(Long tourId, double lat, double lng) {
        List<SseEmitter> tourEmitters = emitters.get(tourId);
        if (tourEmitters == null) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : tourEmitters) {
            try {
                emitter.send(SseEmitter.event().data(Map.of("latitude", lat, "longitude", lng)));
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        tourEmitters.removeAll(dead);
    }

    private void removeEmitter(Long tourId, SseEmitter emitter) {
        List<SseEmitter> tourEmitters = emitters.get(tourId);
        if (tourEmitters != null) {
            tourEmitters.remove(emitter);
        }
    }

    public RouteToStartResponse redirect(String code, double lat, double lng, Integer lastReachedWaypointIndex) {
        Tour tour = tourRepository.findByShareCode(code)
                .or(() -> tourRepository.findByOrganiserCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));

        List<Waypoint> allWaypoints = tour.getSegments().stream()
                .sorted(Comparator.comparingInt(Segment::getOrderIndex))
                .flatMap(s -> s.getWaypoints().stream()
                        .sorted(Comparator.comparingInt(Waypoint::getOrderIndex)))
                .toList();

        int skipCount = (lastReachedWaypointIndex != null) ? lastReachedWaypointIndex + 1 : 0;
        List<Waypoint> remaining = allWaypoints.stream()
                .skip(skipCount)
                .toList();

        if (remaining.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No remaining waypoints");
        }

        Coordinates origin = new Coordinates();
        origin.setLatitude(lat);
        origin.setLongitude(lng);

        List<Waypoint> candidates = remaining.stream()
                .limit(REDIRECT_CANDIDATE_COUNT)
                .toList();

        int rejoinIndex;
        if (candidates.size() == 1) {
            rejoinIndex = 0;
        } else {
            List<Coordinates> candidateCoords = candidates.stream().map(w -> {
                Coordinates c = new Coordinates();
                c.setLatitude(w.getLatitude());
                c.setLongitude(w.getLongitude());
                return c;
            }).toList();

            OSRMTableResponse table = routingService.calculateTable(origin, candidateCoords);
            if (table == null || table.durations() == null || table.durations().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not compute durations");
            }
            List<Double> durations = table.durations().getFirst();
            int bestIndex = 0;
            Double bestDuration = null;
            for (int i = 0; i < durations.size(); i++) {
                Double d = durations.get(i);
                if (d != null && (bestDuration == null || d < bestDuration)) {
                    bestDuration = d;
                    bestIndex = i;
                }
            }
            rejoinIndex = bestIndex;
        }

        List<Waypoint> routeWaypoints = remaining.subList(rejoinIndex, remaining.size());

        List<Coordinates> coordinates = new ArrayList<>();
        coordinates.add(origin);
        for (Waypoint w : routeWaypoints) {
            Coordinates c = new Coordinates();
            c.setLatitude(w.getLatitude());
            c.setLongitude(w.getLongitude());
            coordinates.add(c);
        }

        OSRMResponse osrmResponse = routingService.calculateRoute(coordinates, true);
        if (osrmResponse == null || osrmResponse.routes() == null || osrmResponse.routes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not compute route");
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

        Waypoint rejoinWaypoint = routeWaypoints.getFirst();
        Coordinates rejoinCoords = new Coordinates();
        rejoinCoords.setLatitude(rejoinWaypoint.getLatitude());
        rejoinCoords.setLongitude(rejoinWaypoint.getLongitude());

        com.c15tour.model.Waypoints startDto = new com.c15tour.model.Waypoints();
        startDto.setName(rejoinWaypoint.getName() != null ? rejoinWaypoint.getName() : "Waypoint");
        startDto.setCoordinates(rejoinCoords);
        response.setStartPoint(startDto);

        return response;
    }
}
