package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.model.SegmentRequest;
import com.c15tour.model.SegmentResponse;
import com.c15tour.model.Waypoints;
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

@Mapper(componentModel = "spring", uses = {WaypointMapper.class})
public abstract class SegmentMapper {

    @Autowired
    protected WaypointMapper waypointMapper;

    // Entity -> DTO

    @Mapping(target = "distance", expression = "java(segment.getDistance() != null ? segment.getDistance().longValue() : 0L)")
    @Mapping(target = "duration", expression = "java(segment.getDuration() != null ? segment.getDuration().longValue() : 0L)")
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "estimatedDeparture", ignore = true)
    public abstract SegmentResponse toResponse(Segment segment);

    @AfterMapping
    protected void afterToResponse(Segment segment, @MappingTarget SegmentResponse response) {
        if (segment.getWaypoints() != null) {
            List<Waypoints> waypointDtos = segment.getWaypoints().stream()
                    .sorted(Comparator.comparingInt(Waypoint::getOrderIndex))
                    .map(waypointMapper::toDto)
                    .collect(Collectors.toList());
            response.setWaypoints(waypointDtos);
        }

        if (segment.getEstimatedDeparture() != null) {
            response.setEstimatedDeparture(segment.getEstimatedDeparture().atOffset(ZoneOffset.UTC));
        }
    }

    // DTO -> Entity

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tour", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "distance", constant = "0")
    @Mapping(target = "duration", constant = "0")
    @Mapping(target = "geometry", ignore = true)
    @Mapping(target = "steps", ignore = true)
    @Mapping(target = "estimatedDeparture", ignore = true)
    @Mapping(target = "waypoints", ignore = true)
    @Mapping(target = "breakDuration", expression = "java(request.getBreakDuration() != null ? request.getBreakDuration() : 0)")
    public abstract Segment toEntity(SegmentRequest request);

    @AfterMapping
    protected void afterToEntity(SegmentRequest request, @MappingTarget Segment segment) {
        if (request.getWaypoints() == null) return;

        List<Waypoint> waypointEntities = new ArrayList<>();

        for (int i = 0; i < request.getWaypoints().size(); i++) {
            Waypoints wpDto = request.getWaypoints().get(i);
            if (wpDto.getCoordinates() != null) {
                Waypoint wp = waypointMapper.toEntity(wpDto);
                wp.setOrderIndex(i);
                wp.setSegment(segment);
                waypointEntities.add(wp);
            }
        }
        segment.setWaypoints(waypointEntities);
    }
}