package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.model.SegmentRequest;
import com.c15tour.model.SegmentResponse;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {SegmentMapper.class})
public abstract class TourMapper {

    @Autowired
    protected SegmentMapper segmentMapper;

    // Entity -> DTO

    @Mapping(target = "totalDistance", expression = "java(entity.getTotalDistance() != null ? entity.getTotalDistance() : 0)")
    @Mapping(target = "totalDuration", expression = "java(entity.getTotalDuration() != null ? entity.getTotalDuration() : 0)")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "segments", ignore = true)
    public abstract TourResponse toResponse(Tour entity);

    @AfterMapping
    protected void afterToResponse(Tour entity, @MappingTarget TourResponse response) {
        if (entity.getCreatedAt() != null) {
            response.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getDepartureTime() != null) {
            response.setDepartureTime(entity.getDepartureTime().atOffset(ZoneOffset.UTC));
        }

        if (entity.getSegments() != null) {
            List<SegmentResponse> segmentResponses = entity.getSegments().stream()
                    .sorted(Comparator.comparingInt(Segment::getOrderIndex))
                    .map(segmentMapper::toResponse)
                    .collect(Collectors.toList());
            response.setSegments(segmentResponses);
        }
    }

    // DTO -> Entity (Create)

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalDistance", constant = "0")
    @Mapping(target = "totalDuration", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "shareCode", ignore = true)
    @Mapping(target = "organiserCode", ignore = true)
    @Mapping(target = "organiserJoined", ignore = true)
    @Mapping(target = "organiserSessionToken", ignore = true)
    @Mapping(target = "organiserLat", ignore = true)
    @Mapping(target = "organiserLng", ignore = true)
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "departureTime", expression = "java(request.getDepartureTime() != null ? request.getDepartureTime().toLocalDateTime() : null)")
    public abstract Tour toEntity(TourCreateRequest request);

    @AfterMapping
    protected void afterToEntity(TourCreateRequest request, @MappingTarget Tour tour) {
        tour.setSegments(new ArrayList<>());
        if (request.getSegments() == null) return;

        for (int i = 0; i < request.getSegments().size(); i++) {
            SegmentRequest segmentReq = request.getSegments().get(i);
            Segment segment = segmentMapper.toEntity(segmentReq);
            segment.setOrderIndex(i);
            segment.setTour(tour);
            tour.getSegments().add(segment);
        }
    }

    // DTO -> Entity (Update existing)

    public void updateEntity(Tour existingTour, TourCreateRequest request) {
        existingTour.setName(request.getName());
        existingTour.setDepartureTime(request.getDepartureTime() != null
                ? request.getDepartureTime().toLocalDateTime()
                : null);

        if (existingTour.getSegments() != null) {
            existingTour.getSegments().clear();
        } else {
            existingTour.setSegments(new ArrayList<>());
        }

        if (request.getSegments() != null) {
            for (int i = 0; i < request.getSegments().size(); i++) {
                SegmentRequest segmentReq = request.getSegments().get(i);
                Segment segment = segmentMapper.toEntity(segmentReq);
                segment.setOrderIndex(i);
                segment.setTour(existingTour);
                existingTour.getSegments().add(segment);
            }
        }
    }
}