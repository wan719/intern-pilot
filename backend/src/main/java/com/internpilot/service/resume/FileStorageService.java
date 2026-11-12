package com.internpilot.service.resume;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileInfo store(MultipartFile file, Long userId);
}
