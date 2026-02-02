package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.model.Coordinates;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TourMapper {

    // DTO -> Entity
    public Tour toEntity(TourCreateRequest request) {
        Tour tour = new Tour();
        tour.setName(request.getName());

        if (request.getStartPoint() != null) {
            tour.setStartLatitude(request.getStartPoint().getLatitude());
            tour.setStartLongitude(request.getStartPoint().getLongitude());
        }

        if (request.getEndPoint() != null) {
            tour.setEndLatitude(request.getEndPoint().getLatitude());
            tour.setEndLongitude(request.getEndPoint().getLongitude());
        }

        if (request.getWaypoints() != null) {
            List<Waypoint> waypointEntities = new ArrayList<>();
            for (int i = 0; i < request.getWaypoints().size(); i++) {
                Coordinates coords = request.getWaypoints().get(i);

                Waypoint wp = new Waypoint();
                wp.setLatitude(coords.getLatitude());
                wp.setLongitude(coords.getLongitude());
                wp.setOrderIndex(i); // important de garder l'ordre
                wp.setTour(tour);

                waypointEntities.add(wp);
            }
            tour.setWaypoints(waypointEntities);
        }

        return tour;
    }

    // Entity -> DTO
    public TourResponse toResponse(Tour entity) {
        TourResponse response = new TourResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());

        Coordinates start = new Coordinates();
        start.setLatitude(entity.getStartLatitude());
        start.setLongitude(entity.getStartLongitude());
        response.setStartPoint(start);

        Coordinates end = new Coordinates();
        end.setLatitude(entity.getEndLatitude());
        end.setLongitude(entity.getEndLongitude());
        response.setEndPoint(end);

        if (entity.getWaypoints() != null) {
            List<Coordinates> waypointDtos = entity.getWaypoints().stream()
                    .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                    .map(wp -> {
                        Coordinates c = new Coordinates();
                        c.setLatitude(wp.getLatitude());
                        c.setLongitude(wp.getLongitude());
                        return c;
                    })
                    .collect(Collectors.toList());
            response.setWaypoints(waypointDtos);
        }

        return response;
    }
}