package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
        if (request.getWaypoints() != null) {
            for (int i = 0; i < request.getWaypoints().size(); i++) {
                Waypoints wpDto = request.getWaypoints().get(i);
                if (wpDto.getCoordinates() != null) {
                    Waypoint wp = new Waypoint();
                    wp.setLatitude(wpDto.getCoordinates().getLatitude());
                    wp.setLongitude(wpDto.getCoordinates().getLongitude());
                    wp.setOrderIndex(i);
                    wp.setName(wpDto.getName());
                    wp.setSegment(segment);
                    waypointEntities.add(wp);
                }
            }
        }
        segment.setWaypoints(waypointEntities);
        return segment;
    }

    // --- Entity -> DTO (Response) ---

    private Waypoints toWaypointsDto(Waypoint entity) {
        Waypoints dto = new Waypoints();

        Coordinates coords = new Coordinates();
        coords.setLatitude(entity.getLatitude());
        coords.setLongitude(entity.getLongitude());
        dto.setCoordinates(coords);

        dto.setName(entity.getName());

        return dto;
    }

    private SegmentResponse toSegmentResponse(Segment segment) {
        SegmentResponse response = new SegmentResponse();
        response.setName(segment.getName());
        response.setDistance(segment.getDistance() != null ? segment.getDistance().longValue() : 0L);
        response.setDuration(segment.getDuration() != null ? segment.getDuration().longValue() : 0L);
        response.setGeometry(segment.getGeometry());

        if (segment.getWaypoints() != null) {
            List<Waypoints> waypointDtos = segment.getWaypoints().stream()
                    .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                    .map(this::toWaypointsDto)
                    .collect(Collectors.toList());
            response.setWaypoints(waypointDtos);
        }
        return response;
    }

    public TourResponse toResponse(Tour entity) {
        TourResponse response = new TourResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setShareCode(entity.getShareCode());

        response.setTotalDistance(entity.getTotalDistance() != null ? entity.getTotalDistance() : 0);
        response.setTotalDuration(entity.getTotalDuration() != null ? entity.getTotalDuration() : 0);

        if (entity.getSegments() != null) {
            List<SegmentResponse> segmentResponses = entity.getSegments().stream()
                    .sorted(Comparator.comparingInt(Segment::getOrderIndex))
                    .map(this::toSegmentResponse)
                    .collect(Collectors.toList());
            response.setSegments(segmentResponses);
        }

        return response;
    }
}