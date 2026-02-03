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

    public OSRMResponse CalculateRoute(List<Coordinates> coordinatesList,
                                       Boolean directions) {
        StringBuilder strCoordinates = new StringBuilder();
        for (int i = 0; i < coordinatesList.size(); i++)
        {
            Coordinates co = coordinatesList.get(i);
            strCoordinates.append(co.getLatitude()).append(",").append(co.getLongitude());
            if ((i + 1) < coordinatesList.size())
                strCoordinates.append(";");
        }
        String uri = "/route/v1/driving/"
                + strCoordinates
                + "?"
                + "overview=full"
                + "&geometries=geojson"
                + "&step=" + (directions ? "true" : "false");
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(OSRMResponse.class);
    }

    public OSRMResponse CalculateRoute(List<Coordinates> coordinatesList) {
        return CalculateRoute(coordinatesList, false);
    }
}
