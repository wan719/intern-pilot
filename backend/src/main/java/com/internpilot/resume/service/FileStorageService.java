package com.internpilot.resume.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Optional;

public interface FileStorageService {

    StoredFileInfo store(MultipartFile file, Long userId);

    Optional<Path> findOriginal(Long userId, String storedFileName);
}
