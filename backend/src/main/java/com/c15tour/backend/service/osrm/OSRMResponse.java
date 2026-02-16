package com.c15tour.backend.service.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OSRMResponse(
        String code,
        List<Route> routes,
        List<Waypoint> waypoints
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
          Geometry geometry,
          List<Leg> legs,
          Double distance,
          Double duration,
          String weight_name,
          Double weight
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Geometry(
            List<List<Double>> coordinates,
            String type
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Leg(
            List<Step> steps,
            Double distance,
            Float duration,
            String summary,
            Double weight
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            List<Intersection> intersections,
            String driving_side,
            Geometry geometry,
            String mode,
            Float duration,
            Maneuver maneuver,
            Double weight,
            Double distance,
            String name
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Maneuver(
          Integer exit,
          Float bearing_after,
          String type,
          String modifier,
          Float bearing_before,
          List<Double> location
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Intersection(
            Integer out,
            List<Boolean> entry,
            List<Integer> bearings,
            List<Double> location,
            Integer in
    ){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Waypoint(
            String hint,
            Double distance,
            String name,
            List<Double> location
    ){}
}