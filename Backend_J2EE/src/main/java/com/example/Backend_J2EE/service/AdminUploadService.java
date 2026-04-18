package com.example.Backend_J2EE.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class AdminUploadService {

    private final FileStorageService fileStorageService;

    public AdminUploadService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public Map<String, String> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File la bat buoc");
        }
        return Map.of("url", fileStorageService.store(file, "images"));
    }
}
