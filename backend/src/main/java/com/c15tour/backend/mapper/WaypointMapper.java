package com.c15tour.backend.mapper;

import com.c15tour.backend.entity.Waypoint;
import com.c15tour.model.Coordinates;
import com.c15tour.model.Waypoints;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface WaypointMapper {

    // Entity -> DTO

    @Mapping(target = "coordinates", source = ".")
    @Mapping(target = "estimatedArrival", ignore = true)
    Waypoints toDto(Waypoint entity);

    default Coordinates toCoordinates(Waypoint entity) {
        if (entity == null) return null;
        Coordinates coords = new Coordinates();
        coords.setLatitude(entity.getLatitude());
        coords.setLongitude(entity.getLongitude());
        return coords;
    }

    @AfterMapping
    default void mapEstimatedArrival(Waypoint entity, @MappingTarget Waypoints dto) {
        if (entity.getEstimatedArrival() != null) {
            dto.setEstimatedArrival(entity.getEstimatedArrival().atOffset(ZoneOffset.UTC));
        }
    }

    // DTO -> Entity

    @Mapping(target = "latitude", source = "coordinates.latitude")
    @Mapping(target = "longitude", source = "coordinates.longitude")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "orderIndex", ignore = true)
    @Mapping(target = "estimatedArrival", ignore = true)
    Waypoint toEntity(Waypoints dto);
}