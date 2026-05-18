package com.c15tour.backend.controller;

import com.c15tour.api.SystemApi;
import com.c15tour.backend.service.RoutingService;
import com.c15tour.backend.service.osrm.OSRMResponse;
import com.c15tour.model.Coordinates;
import com.c15tour.model.Message;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PingController implements SystemApi {

    private final RoutingService routingService;

    public PingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @Override
    public ResponseEntity<Message> ping() {
        Message response = new Message();

        try {
            // Coordonnées Nantes
            Coordinates start = new Coordinates();
            start.setLongitude(-1.5536);
            start.setLatitude(47.2184);

            // Coordonnées Angers
            Coordinates end = new Coordinates();
            end.setLongitude(-0.5638);
            end.setLatitude(47.4784);

            // Appel au service
            OSRMResponse osrmResult = routingService.calculateRoute(List.of(start, end));

            if (osrmResult != null && !osrmResult.routes().isEmpty()) {
                double distanceKm = osrmResult.routes().get(0).distance() / 1000;
                response.setContent("SUCCÈS ! OSRM a calculé une route de " + distanceKm + " km.");
            } else {
                response.setContent("ÉCHEC : OSRM a répondu mais pas de route trouvée.");
            }
        } catch (Exception e) {
            response.setContent("ERREUR : " + e.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(response);
    }
}