package com.c15tour.backend.controller;

import com.c15tour.backend.entity.AudioMessage;
import com.c15tour.backend.entity.Segment;
import com.c15tour.backend.entity.Tour;
import com.c15tour.backend.entity.Waypoint;
import com.c15tour.backend.repository.AudioMessageRepository;
import com.c15tour.backend.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class AudioMessageControllerIntegrationTest {

    @TempDir
    static Path tempDir;

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureStoragePath(DynamicPropertyRegistry registry) {
        registry.add("audio.storage.path", () -> tempDir.toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private AudioMessageRepository audioMessageRepository;

    private Tour savedTour;

    @BeforeEach
    void setUp() {
        Tour tour = new Tour();
        tour.setName("Audio Test Tour");
        tour.setTotalDistance(100);
        tour.setTotalDuration(3600);
        tour.setShareCode("AUDIO1");
        tour.setOrganiserCode("ORGAUD");
        tour.setOrganiserJoined(true);
        tour.setOrganiserSessionToken("valid-audio-token");
        tour.setOrganiserTokenExpiresAt(LocalDateTime.now().plusHours(24));

        Segment segment = new Segment();
        segment.setName("Seg");
        segment.setTour(tour);
        segment.setDistance(100);
        segment.setDuration(3600);
        segment.setOrderIndex(0);

        Waypoint wp1 = new Waypoint();
        wp1.setLatitude(47.0);
        wp1.setLongitude(-1.5);
        wp1.setOrderIndex(0);
        wp1.setSegment(segment);

        Waypoint wp2 = new Waypoint();
        wp2.setLatitude(48.0);
        wp2.setLongitude(2.3);
        wp2.setOrderIndex(1);
        wp2.setSegment(segment);

        segment.setWaypoints(new ArrayList<>(List.of(wp1, wp2)));
        tour.setSegments(new ArrayList<>(List.of(segment)));

        savedTour = tourRepository.save(tour);
    }

    @Test
    void postAudioMessage_ValidUpload_ShouldReturn201AndPersist() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "message.ogg", "audio/ogg", "fake-ogg-content".getBytes()
        );

        mockMvc.perform(multipart("/tours/share/ORGAUD/audio-messages")
                        .file(file)
                        .header("X-Session-Token", "valid-audio-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("/audio/")))
                .andExpect(jsonPath("$.createdAt").exists());

        List<AudioMessage> messages = audioMessageRepository.findByTourShareCodeOrderByCreatedAtDesc("AUDIO1");
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getFileName()).endsWith(".ogg");
    }

    @Test
    void postAudioMessage_InvalidToken_ShouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "message.ogg", "audio/ogg", "fake-ogg-content".getBytes()
        );

        mockMvc.perform(multipart("/tours/share/AUDIO1/audio-messages")
                        .file(file)
                        .header("X-Session-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postAudioMessage_WrongCode_ShouldReturn403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "message.ogg", "audio/ogg", "fake-ogg-content".getBytes()
        );

        mockMvc.perform(multipart("/tours/share/WRONG1/audio-messages")
                        .file(file)
                        .header("X-Session-Token", "valid-audio-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAudioMessage_FileTooLarge_ShouldReturn413() throws Exception {
        byte[] bigFile = new byte[3 * 1024 * 1024]; // 3 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "message.ogg", "audio/ogg", bigFile
        );

        mockMvc.perform(multipart("/tours/share/ORGAUD/audio-messages")
                        .file(file)
                        .header("X-Session-Token", "valid-audio-token"))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void postAudioMessage_InvalidFormat_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "message.mp3", "audio/mpeg", "fake-mp3-content".getBytes()
        );

        mockMvc.perform(multipart("/tours/share/ORGAUD/audio-messages")
                        .file(file)
                        .header("X-Session-Token", "valid-audio-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAudioMessages_ShouldReturnHistoryOrderedByDateDesc() throws Exception {
        AudioMessage msg1 = new AudioMessage();
        msg1.setTour(savedTour);
        msg1.setFileName("aaa-uuid.ogg");
        audioMessageRepository.save(msg1);

        AudioMessage msg2 = new AudioMessage();
        msg2.setTour(savedTour);
        msg2.setFileName("bbb-uuid.ogg");
        audioMessageRepository.save(msg2);

        mockMvc.perform(get("/tours/share/AUDIO1/audio-messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].url").value("/audio/bbb-uuid.ogg"))
                .andExpect(jsonPath("$[1].url").value("/audio/aaa-uuid.ogg"));
    }

    @Test
    void getAudioMessages_TourNotFound_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/tours/share/XXXXXX/audio-messages"))
                .andExpect(status().isNotFound());
    }
}
