package com.c15tour.backend.repository;

import com.c15tour.backend.entity.AudioMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AudioMessageRepository extends JpaRepository<AudioMessage, Long> {
    List<AudioMessage> findByTourShareCodeOrderByCreatedAtDesc(String shareCode);
}
