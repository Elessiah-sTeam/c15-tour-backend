package com.c15tour.backend.repository;

import com.c15tour.backend.entity.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    Optional<Tour> findByShareCode(String shareCode);
}