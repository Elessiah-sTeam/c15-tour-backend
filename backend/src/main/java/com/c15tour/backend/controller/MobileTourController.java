package com.c15tour.backend.controller;

import com.c15tour.backend.service.MobileTourService;
import com.c15tour.model.JoinResponse;
import com.c15tour.model.OrganiserPositionRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class MobileTourController {

    private final MobileTourService mobileTourService;

    public MobileTourController(MobileTourService mobileTourService) {
        this.mobileTourService = mobileTourService;
    }

    @PostMapping("/tours/share/{code}/join")
    public ResponseEntity<JoinResponse> joinTour(@PathVariable String code) {
        return ResponseEntity.ok(mobileTourService.join(code));
    }

    @PutMapping("/tours/share/{code}/organiser-position")
    public ResponseEntity<Void> updateOrganiserPosition(
            @PathVariable String code,
            @RequestHeader("X-Session-Token") String xSessionToken,
            @RequestBody OrganiserPositionRequest body) {
        mobileTourService.updateOrganiserPosition(code, xSessionToken, body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/tours/share/{code}/organiser-position/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrganiserPosition(@PathVariable String code) {
        return mobileTourService.streamOrganiserPosition(code);
    }
}
