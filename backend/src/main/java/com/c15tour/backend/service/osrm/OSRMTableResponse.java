package com.c15tour.backend.service.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OSRMTableResponse(
        String code,
        List<List<Double>> durations,
        List<OSRMResponse.Waypoint> sources,
        List<OSRMResponse.Waypoint> destinations
) {}
