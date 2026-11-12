package com.internpilot.resume.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileInfo store(MultipartFile file, Long userId);
}
