package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.model.Coordinates;
import com.c15tour.model.SegmentRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import com.c15tour.model.Waypoints;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TourMapper {

    // --- DTO -> Entity (Create) ---

    public Tour toEntity(TourCreateRequest request) {
        Tour tour = new Tour();
        mapRequestToEntity(request, tour);
        tour.setTotalDistance(0);
        tour.setTotalDuration(0);
        return tour;
    }

    public void updateEntity(Tour existingTour, TourCreateRequest request) {
        mapRequestToEntity(request, existingTour);
    }

    private void mapRequestToEntity(TourCreateRequest request, Tour tour) {
        tour.setName(request.getName());

        if (tour.getSegments() != null) {
            tour.getSegments().clear();
        } else {
            tour.setSegments(new ArrayList<>());
        }

        if (request.getSegments() != null) {
            for (int i = 0; i < request.getSegments().size(); i++) {
                SegmentRequest segmentReq = request.getSegments().get(i);
                Segment segment = mapSegmentRequestToEntity(segmentReq, i, tour);
                tour.getSegments().add(segment);
            }
        }
    }

    private Segment mapSegmentRequestToEntity(SegmentRequest request, int index, Tour tour) {
        Segment segment = new Segment();
        segment.setName(request.getName());
        segment.setOrderIndex(index);
        segment.setTour(tour);

        segment.setDistance(0);
        segment.setDuration(0);

        List<Waypoint> waypointEntities = new ArrayList<>();
        if (request.getWaypoint() != null) {
            for (int i = 0; i < request.getWaypoint().size(); i++) {
                Waypoints wpDto = request.getWaypoint().get(i);
                if (wpDto.getCoordinates() != null) {
                    Waypoint wp = new Waypoint();
                    wp.setLatitude(wpDto.getCoordinates().getLatitude());
                    wp.setLongitude(wpDto.getCoordinates().getLongitude());
                    wp.setOrderIndex(i);
                    wp.setSegment(segment);
                    waypointEntities.add(wp);
                }
            }
        }
        segment.setWaypoints(waypointEntities);
        return segment;
    }

    // --- Entity -> DTO (Response) ---

    public TourResponse toResponse(Tour entity) {
        TourResponse response = new TourResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setShareCode(entity.getShareCode());

        response.setDistance(entity.getTotalDistance() != null ? entity.getTotalDistance().doubleValue() : 0.0);
        response.setDuration(entity.getTotalDuration() != null ? entity.getTotalDuration().doubleValue() : 0.0);

        List<Segment> sortedSegments = entity.getSegments().stream()
                .sorted(Comparator.comparingInt(Segment::getOrderIndex))
                .toList();

        List<Coordinates> allCoordinates = new ArrayList<>();

        for (Segment seg : sortedSegments) {
            if (seg.getWaypoints() != null) {
                seg.getWaypoints().stream()
                        .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                        .forEach(wp -> {
                            Coordinates c = new Coordinates();
                            c.setLatitude(wp.getLatitude());
                            c.setLongitude(wp.getLongitude());
                            allCoordinates.add(c);
                        });
            }
        }

        if (!allCoordinates.isEmpty()) {
            response.setStartPoint(allCoordinates.getFirst());

            response.setEndPoint(allCoordinates.getLast());

            if (allCoordinates.size() > 2) {
                List<Coordinates> intermediates = new ArrayList<>(allCoordinates.subList(1, allCoordinates.size() - 1));
                response.setWaypoints(intermediates);
            } else {
                response.setWaypoints(new ArrayList<>());
            }
        }

        response.setGeometry(null);

        return response;
    }
}