package com.c15tour.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "waypoints")
public class Waypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "segment_id", nullable = false)
    private Segment segment;

}
