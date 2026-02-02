package com.c15tour.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours")
@Data
public class Tour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_lat", nullable = false)
    private Double startLatitude;

    @Column(name = "start_lon", nullable = false)
    private Double startLongitude;

    @Column(name = "end_lat", nullable = false)
    private Double endLatitude;

    @Column(name = "end_lon", nullable = false)
    private Double endLongitude;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Waypoint> waypoints = new ArrayList<>();

}