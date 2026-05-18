package com.c15tour.backend.service;

import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class RoutingService {
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
            System.err.println("ERREUR HTTP OSRM : " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            e.printStackTrace();
            // 4xx/5xx: tu peux logguer e.getStatusCode() et e.getResponseBodyAsString()
            return null;
        } catch (org.springframework.web.client.RestClientException e) {
            System.err.println("ERREUR CLIENT OSRM (Réseau ou Parsing) : " + e.getMessage());
            e.printStackTrace();
            // réseau / timeout / etc.
            return null;
        }
    }

    public OSRMResponse calculateRoute(List<Coordinates> coordinatesList) {
        return calculateRoute(coordinatesList, false);
    }
}
