package com.c15tour.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Segment> segments = new ArrayList<>();

    @Column(name = "share_code", unique = true, length = 6)
    private String shareCode;

    @Column(name = "organiser_code", unique = true, length = 6)
    private String organiserCode;

    @Column(name = "organiser_joined", nullable = false)
    private boolean organiserJoined = false;

    @Column(name = "organiser_session_token", unique = true)
    private String organiserSessionToken;

    @Column(name = "organiser_token_expires_at")
    private LocalDateTime organiserTokenExpiresAt;

    @Column(name = "organiser_lat")
    private Double organiserLat;

    @Column(name = "organiser_lng")
    private Double organiserLng;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "draft", nullable = false)
    private boolean draft = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User owner;
}