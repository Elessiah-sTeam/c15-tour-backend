package com.c15tour.backend.controller;

import com.c15tour.api.ToursApi;
import com.c15tour.backend.service.TourService;
import com.c15tour.model.PatchDepartureTimeRequest;
import com.c15tour.model.TourCreateRequest;
import com.c15tour.model.TourPageResponse;
import com.c15tour.model.TourResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TourController implements ToursApi {

    private final TourService tourService;

    public TourController(TourService tourService) {
        this.tourService = tourService;
    }

    @Override
    public ResponseEntity<TourResponse> createTour(TourCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return new ResponseEntity<>(tourService.create(request, auth.getName()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<TourResponse> updateTour(Long id, TourCreateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return tourService.update(id, request, auth.getName(), isAdmin(auth))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> patchTourDepartureTime(Long id, PatchDepartureTimeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return tourService.patchDepartureTime(id, request, auth.getName(), isAdmin(auth))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<Void> deleteTour(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return tourService.delete(id, auth.getName(), isAdmin(auth))
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<TourPageResponse> getAllTours(Integer page, Integer size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(tourService.getAll(
                page != null ? page : 0,
                size != null ? size : 20,
                auth.getName(),
                isAdmin(auth)
        ));
    }

    @Override
    public ResponseEntity<TourResponse> getTourById(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return tourService.getById(id, auth.getName(), isAdmin(auth))
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    public ResponseEntity<TourResponse> getTourByShareCode(String code) {
        return tourService.getByShareCode(code)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
