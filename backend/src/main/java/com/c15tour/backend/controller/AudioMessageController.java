package com.c15tour.backend.controller;

import com.c15tour.backend.service.AudioMessageService;
import com.c15tour.backend.service.AudioStorageService;
import com.c15tour.model.AudioMessageResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class AudioMessageController {

    private final AudioMessageService audioMessageService;
    private final AudioStorageService audioStorageService;

    public AudioMessageController(AudioMessageService audioMessageService,
                                  AudioStorageService audioStorageService) {
        this.audioMessageService = audioMessageService;
        this.audioStorageService = audioStorageService;
    }

    @PostMapping(value = "/tours/share/{code}/audio-messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioMessageResponse> postAudioMessage(
            @PathVariable String code,
            @RequestHeader("X-Session-Token") String xSessionToken,
            @RequestPart("file") MultipartFile file) {
        AudioMessageResponse response = audioMessageService.upload(code, xSessionToken, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/tours/share/{code}/audio-messages")
    public ResponseEntity<List<AudioMessageResponse>> getAudioMessages(@PathVariable String code) {
        return ResponseEntity.ok(audioMessageService.getHistory(code));
    }

    @GetMapping("/audio/{filename}")
    public ResponseEntity<Resource> getAudioFile(@PathVariable String filename) {
        Resource resource = audioStorageService.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("audio/ogg"))
                .body(resource);
    }
}
