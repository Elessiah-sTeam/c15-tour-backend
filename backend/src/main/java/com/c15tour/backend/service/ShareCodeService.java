package com.c15tour.backend.service;

import com.c15tour.backend.repository.TourRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class ShareCodeService {

    private final TourRepository tourRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    public ShareCodeService(TourRepository tourRepository) {
        this.tourRepository = tourRepository;
    }

    public String generateUniqueShareCode() {
        while (true) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
            }
            String shareCode = sb.toString();
            if (tourRepository.findByShareCode(shareCode).isEmpty()) {
                return shareCode;
            }
        }
    }
}
