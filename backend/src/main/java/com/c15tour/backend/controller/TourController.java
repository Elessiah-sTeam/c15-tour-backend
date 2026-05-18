package com.c15tour.backend.controller;

import com.c15tour.api.ToursApi;
import com.c15tour.backend.service.TourService;
import com.c15tour.model.PatchDepartureTimeRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourPageResponse;
import com.c15tour.model.TourResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TourController implements ToursApi {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @Override
    public ResponseEntity<TourResponse> createTour(TourCreateRequest request) {
        return new ResponseEntity<>(tourService.create(request), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TourResponse> updateTour(Long id, TourCreateRequest request) {
        return tourService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> patchTourDepartureTime(Long id, PatchDepartureTimeRequest request) {
        return tourService.patchDepartureTime(id, request)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<Void> deleteTour(Long id) {
        return tourService.delete(id)
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<TourPageResponse> getAllTours(Integer page, Integer size) {
        return ResponseEntity.ok(tourService.getAll(
                page != null ? page : 0,
                size != null ? size : 20
        ));
    }

    @Override
    public ResponseEntity<TourResponse> getTourById(Long id) {
        return tourService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> getTourByShareCode(String code) {
        return tourService.getByShareCode(code)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
