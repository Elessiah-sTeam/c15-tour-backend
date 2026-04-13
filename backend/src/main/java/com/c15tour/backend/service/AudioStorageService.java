package com.c15tour.backend.service;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface AudioStorageService {
    String store(InputStream inputStream, String filename);
    Resource load(String filename);
}
