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

    @Column(name = "total_distance", nullable = false)
    private Integer totalDistance;

    @Column(name = "total_duration", nullable = false)
    private Integer totalDuration; // En secondes

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Segment> segments = new ArrayList<>();

    @Column(name = "share_code", unique = true, length = 6)
    private String shareCode;
}