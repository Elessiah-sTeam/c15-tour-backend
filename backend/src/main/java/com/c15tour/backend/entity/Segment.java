package com.c15tour.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "segment")
@Data
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer distance;

    @Column(nullable = false)
    private Integer duration; // En secondes

    @OneToMany(mappedBy = "segment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex asc")
    private List<Waypoint> waypoints = new ArrayList<>();

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private String geometry;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "break_duration")
    private Integer breakDuration;

    @Column(name = "estimated_departure")
    private LocalDateTime estimatedDeparture;
}
