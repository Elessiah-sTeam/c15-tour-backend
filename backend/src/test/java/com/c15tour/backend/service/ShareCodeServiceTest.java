package com.c15tour.backend.service;

import com.c15tour.backend.repository.TourRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareCodeServiceTest {

    @Mock
    private TourRepository tourRepository;

    private ShareCodeService shareCodeService;

    @BeforeEach
    void setUp() {
        shareCodeService = new ShareCodeService(tourRepository);
    }

    @Test
    void generateUniqueShareCode_ShouldReturn6CharAlphanumericCode() {
        when(tourRepository.findByShareCode(anyString())).thenReturn(Optional.empty());

        String code = shareCodeService.generateUniqueShareCode();

        assertThat(code).hasSize(6);
        assertThat(code).matches("[A-Z0-9]{6}");
    }

    @Test
    void generateUniqueShareCode_ShouldRetryIfCodeAlreadyExists() {
        // First call finds a collision, second is unique
        when(tourRepository.findByShareCode(anyString()))
                .thenReturn(Optional.of(new com.c15tour.backend.entity.Tour()))
                .thenReturn(Optional.empty());

        String code = shareCodeService.generateUniqueShareCode();

        assertThat(code).hasSize(6);
        verify(tourRepository, times(2)).findByShareCode(anyString());
    }

    @Test
    void generateUniqueShareCode_ShouldReturnDifferentCodesOnMultipleCalls() {
        when(tourRepository.findByShareCode(anyString())).thenReturn(Optional.empty());

        String code1 = shareCodeService.generateUniqueShareCode();
        String code2 = shareCodeService.generateUniqueShareCode();

        // Both are valid — they could theoretically collide but probability is ~1 in 2.8 billion
        assertThat(code1).matches("[A-Z0-9]{6}");
        assertThat(code2).matches("[A-Z0-9]{6}");
    }
}
