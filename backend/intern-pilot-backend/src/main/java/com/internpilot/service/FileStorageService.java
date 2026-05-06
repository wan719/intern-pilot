package com.internpilot.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileInfo store(MultipartFile file, Long userId);
}
