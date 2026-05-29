package com.c15tour.backend.service;

import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.backend.service.osrm.OSRMTableResponse;
import com.c15tour.model.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RoutingService {
    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

    private RestClient restClient;

    @Autowired
    public RoutingService(RestClient restClient)
    {
        this.restClient = restClient;
    }

    public OSRMResponse calculateRoute(List<Coordinates> coordinatesList,
                                       Boolean directions) {
        if (coordinatesList == null || coordinatesList.size() < 2) return null;

        String coords = coordinatesList.stream()
                // OSRM: lon,lat
                .map(c -> c.getLongitude() + "," + c.getLatitude())
                .collect(java.util.stream.Collectors.joining(";"));
        try {
            OSRMResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/{coords}")
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .queryParam("steps", directions)
                            .build(coords))
                    .retrieve()
                    .body(OSRMResponse.class);

            if (response != null && "Ok".equalsIgnoreCase(response.code())) return response;
            return null;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            logger.error("OSRM HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (org.springframework.web.client.RestClientException e) {
            logger.error("OSRM client error (network/parsing): {}", e.getMessage(), e);
            return null;
        }
    }

    public OSRMResponse calculateRoute(List<Coordinates> coordinatesList) {
        return calculateRoute(coordinatesList, false);
    }

    /**
     * Calls OSRM Table API to get durations from a single origin to N destinations.
     * Returns durations[0][i] = seconds from origin to destination i (null if unreachable).
     */
    public OSRMTableResponse calculateTable(Coordinates origin, List<Coordinates> destinations) {
        if (destinations == null || destinations.isEmpty()) return null;

        List<Coordinates> all = new java.util.ArrayList<>();
        all.add(origin);
        all.addAll(destinations);

        String coords = all.stream()
                .map(c -> c.getLongitude() + "," + c.getLatitude())
                .collect(Collectors.joining(";"));

        String destinationIndices = IntStream.rangeClosed(1, destinations.size())
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(";"));

        // Build raw URI to prevent Spring from percent-encoding the semicolons in query params
        String rawUri = "/table/v1/driving/" + coords + "?sources=0&destinations=" + destinationIndices;

        try {
            OSRMTableResponse response = restClient.get()
                    .uri(rawUri)
                    .retrieve()
                    .body(OSRMTableResponse.class);

            if (response != null && "Ok".equalsIgnoreCase(response.code())) return response;
            return null;

        } catch (org.springframework.web.client.RestClientResponseException e) {
            logger.error("OSRM Table HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (org.springframework.web.client.RestClientException e) {
            logger.error("OSRM Table client error: {}", e.getMessage(), e);
            return null;
        }
    }
}
