package com.c15tour.backend.service;

import com.c15tour.backend.entity.AudioMessage;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.repository.AudioMessageRepository;
import com.c15tour.backend.repository.TourRepository;
import com.c15tour.model.AudioMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AudioMessageService {

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2 MB

    private final TourRepository tourRepository;
    private final AudioMessageRepository audioMessageRepository;
    private final AudioStorageService audioStorageService;
    private final MobileTourService mobileTourService;

    public AudioMessageService(TourRepository tourRepository,
                               AudioMessageRepository audioMessageRepository,
                               AudioStorageService audioStorageService,
                               MobileTourService mobileTourService) {
        this.tourRepository = tourRepository;
        this.audioMessageRepository = audioMessageRepository;
        this.audioStorageService = audioStorageService;
        this.mobileTourService = mobileTourService;
    }

    public AudioMessageResponse upload(String code, String sessionToken, MultipartFile file) {
        Tour tour = tourRepository.findByOrganiserSessionToken(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session token"));

        if (tour.getOrganiserTokenExpiresAt() == null || LocalDateTime.now().isAfter(tour.getOrganiserTokenExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session token expired");
        }

        if (!code.equals(tour.getShareCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session token does not match this tour");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds 2 MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".ogg")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only .ogg files are accepted");
        }

        String storedFilename = UUID.randomUUID() + ".ogg";
        try {
            audioStorageService.store(file.getInputStream(), storedFilename);
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file");
        }

        AudioMessage message = new AudioMessage();
        message.setTour(tour);
        message.setFileName(storedFilename);
        audioMessageRepository.saveAndFlush(message);

        String url = "/audio/" + storedFilename;
        mobileTourService.pushAudioMessageEvent(tour.getId(), message.getId(), url, message.getCreatedAt());

        return toResponse(message);
    }

    public List<AudioMessageResponse> getHistory(String code) {
        if (!tourRepository.existsByShareCode(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found");
        }
        return audioMessageRepository.findByTourShareCodeOrderByCreatedAtDesc(code)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private AudioMessageResponse toResponse(AudioMessage message) {
        AudioMessageResponse response = new AudioMessageResponse();
        response.setId(message.getId());
        response.setUrl("/audio/" + message.getFileName());
        response.setCreatedAt(message.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
        return response;
    }
}
