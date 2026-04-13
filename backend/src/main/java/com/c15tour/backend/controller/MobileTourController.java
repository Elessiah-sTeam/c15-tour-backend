package com.c15tour.backend.controller;

import com.c15tour.api.MobileApi;
import com.c15tour.backend.service.MobileTourService;
import com.c15tour.model.JoinResponse;
import com.c15tour.model.OrganiserPositionRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MobileTourController implements MobileApi {

    private final MobileTourService mobileTourService;

    public MobileTourController(MobileTourService mobileTourService) {
        this.mobileTourService = mobileTourService;
    }

    @Override
    public ResponseEntity<JoinResponse> joinTour(String code) {
        return ResponseEntity.ok(mobileTourService.join(code));
    }

    @Override
    public ResponseEntity<Void> updateOrganiserPosition(String code, String xSessionToken, OrganiserPositionRequest body) {
        mobileTourService.updateOrganiserPosition(code, xSessionToken, body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/tours/share/{code}/organiser-position/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrganiserPosition(@PathVariable String code) {
        return mobileTourService.streamOrganiserPosition(code);
    }
}
