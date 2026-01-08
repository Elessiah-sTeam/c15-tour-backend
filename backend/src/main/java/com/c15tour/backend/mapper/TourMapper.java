package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Tour;
import com.c15tour.model.Coordinates;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import org.springframework.stereotype.Component;

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

        return response;
    }
}