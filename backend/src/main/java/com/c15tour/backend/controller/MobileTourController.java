package com.c15tour.backend.controller;

import com.c15tour.api.MobileApi;
import com.c15tour.backend.service.AudioMessageService;
import com.c15tour.backend.service.AudioStorageService;
import com.c15tour.backend.service.MobileTourService;
import com.c15tour.model.AudioMessageResponse;
import com.c15tour.model.JoinResponse;
import com.c15tour.model.OrganiserPositionRequest;
import com.c15tour.model.RedirectRequest;
import com.c15tour.model.RedirectTourResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class MobileTourController implements MobileApi {

    private final MobileTourService mobileTourService;
    private final AudioMessageService audioMessageService;
    private final AudioStorageService audioStorageService;

    public MobileTourController(MobileTourService mobileTourService,
                                AudioMessageService audioMessageService,
                                AudioStorageService audioStorageService) {
        this.mobileTourService = mobileTourService;
        this.audioMessageService = audioMessageService;
        this.audioStorageService = audioStorageService;
    }

    @Override
    public ResponseEntity<JoinResponse> joinTour(String code) {
        return ResponseEntity.ok(mobileTourService.join(code));
    }

    @Override
    public ResponseEntity<Void> updateOrganiserPosition(String code, String xSessionToken, OrganiserPositionRequest organiserPositionRequest) {
        mobileTourService.updateOrganiserPosition(code, xSessionToken, organiserPositionRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/tours/share/{code}/organiser-position/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrganiserPosition(@PathVariable String code) {
        return mobileTourService.streamOrganiserPosition(code);
    }

    @Override
    public ResponseEntity<AudioMessageResponse> postAudioMessage(String code, String xSessionToken, MultipartFile file) {
        AudioMessageResponse response = audioMessageService.upload(code, xSessionToken, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<AudioMessageResponse>> getAudioMessages(String code) {
        return ResponseEntity.ok(audioMessageService.getHistory(code));
    }

    @Override
    public ResponseEntity<RedirectTourResponse> redirectToTour(String code, RedirectRequest redirectRequest) {
        RedirectTourResponse response = mobileTourService.redirect(
                code,
                redirectRequest.getLatitude(),
                redirectRequest.getLongitude(),
                redirectRequest.getLastReachedWaypointIndex()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Resource> getAudioFile(String filename) {
        Resource resource = audioStorageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("audio/ogg"))
                .body(resource);
    }
}
