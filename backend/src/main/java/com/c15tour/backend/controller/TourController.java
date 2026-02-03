package com.c15tour.backend.controller;

import com.c15tour.api.ToursApi;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.backend.mapper.TourMapper;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TourController implements ToursApi {

    private final TourRepository tourRepository;
    private final TourMapper tourMapper;

    public TourController(TourRepository tourRepository, TourMapper tourMapper) {
        this.tourRepository = tourRepository;
        this.tourMapper = tourMapper;
    }

    @Override
    public ResponseEntity<TourResponse> createTour(TourCreateRequest tourCreateRequest) {
        Tour tourEntity = tourMapper.toEntity(tourCreateRequest);

        Tour savedTour = tourRepository.save(tourEntity);

        TourResponse response = tourMapper.toResponse(savedTour);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteTour(Long id) {
        if (tourRepository.existsById(id)) {
            tourRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<TourResponse>> getAllTours() {
        List<TourResponse> tourResponses = tourRepository.findAll().stream()
                .map(tourMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tourResponses);
    }

    @Override
    public ResponseEntity<TourResponse> getTourById(Long id) {
        return tourRepository.findById(id)
                .map(tourMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> updateTour(Long id, @Valid TourCreateRequest tourCreateRequest) {
        return tourRepository.findById(id)
                .map(existingTour -> {
                    existingTour.setName(tourCreateRequest.getName());
                    existingTour.setStartLatitude(tourCreateRequest.getStartPoint().getLatitude());
                    existingTour.setEndLatitude(tourCreateRequest.getEndPoint().getLatitude());
                    existingTour.setStartLongitude(tourCreateRequest.getStartPoint().getLongitude());
                    existingTour.setEndLongitude(tourCreateRequest.getEndPoint().getLongitude());

                    Tour savedTour = tourRepository.save(existingTour);
                    return ResponseEntity.ok(tourMapper.toResponse(savedTour));
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> getTourByShareCode(String code) {
        return tourRepository.findByShareCode(code)
                .map(tourMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}