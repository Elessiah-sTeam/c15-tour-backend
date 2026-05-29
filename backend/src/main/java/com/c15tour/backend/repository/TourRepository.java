package com.c15tour.backend.repository;

import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    Optional<Tour> findByShareCode(String shareCode);
    Optional<Tour> findByOrganiserCode(String organiserCode);
    Optional<Tour> findByOrganiserSessionToken(String organiserSessionToken);
    boolean existsByShareCode(String shareCode);
    Page<Tour> findByOwner(User owner, Pageable pageable);
    Optional<Tour> findByIdAndOwner(Long id, User owner);
    boolean existsByIdAndOwner(Long id, User owner);
}