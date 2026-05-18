package com.c15tour.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalAudioStorageService implements AudioStorageService {

    private final Path storageRoot;

    public LocalAudioStorageService(@Value("${audio.storage.path}") String storagePath) {
        this.storageRoot = Paths.get(storagePath);
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot initialize audio storage directory: " + storagePath, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename) {
        Path destination = storageRoot.resolve(filename).normalize();
        if (!destination.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        try {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store audio file");
        }
        return filename;
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = storageRoot.resolve(filename).normalize();
            if (!file.startsWith(storageRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio file not found");
            }
            return resource;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audio file not found");
        }
    }
}
