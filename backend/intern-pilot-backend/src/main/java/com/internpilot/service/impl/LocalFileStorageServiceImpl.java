package com.internpilot.service.impl;

import com.internpilot.config.FileStorageConfig;
import com.internpilot.enums.FileTypeEnum;
import com.internpilot.exception.FileParseException;
import com.internpilot.service.FileStorageService;
import com.internpilot.service.StoredFileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final String PDF_MIME = "application/pdf";
    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DOCX_MIME_ALT = "application/octet-stream";

    private final FileStorageConfig fileStorageConfig;

    @Override
    public StoredFileInfo store(MultipartFile file, Long userId) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        FileTypeEnum fileType = FileTypeEnum.fromExtension(extension);

        if (fileType == null) {
            throw new FileParseException("仅支持 PDF 或 DOCX 文件");
        }

        String storedFileName = generateStoredFileName(userId, extension);
        Path userDir = Paths.get(fileStorageConfig.getUploadDir(), "user-" + userId);

        try {
            Files.createDirectories(userDir);
            Path targetPath = userDir.resolve(storedFileName).normalize();
            file.transferTo(targetPath.toFile());

            return new StoredFileInfo(
                    originalFileName,
                    storedFileName,
                    targetPath.toString(),
                    fileType.getCode(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new FileParseException("简历文件保存失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileParseException("上传文件不能为空");
        }

        Long maxSize = fileStorageConfig.getMaxSize();
        if (maxSize != null && file.getSize() > maxSize) {
            throw new FileParseException("文件大小不能超过 10MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileParseException("文件名不能为空");
        }

        String extension = getExtension(originalFileName);
        FileTypeEnum fileType = FileTypeEnum.fromExtension(extension);
        if (fileType == null) {
            throw new FileParseException("仅支持 PDF 或 DOCX 文件");
        }

        if (!isSupportedMimeType(file.getContentType(), fileType)) {
            throw new FileParseException("仅支持 PDF 或 DOCX 文件");
        }
    }

    private boolean isSupportedMimeType(String contentType, FileTypeEnum fileType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }

        return switch (fileType) {
            case PDF -> PDF_MIME.equalsIgnoreCase(contentType);
            case DOCX -> DOCX_MIME.equalsIgnoreCase(contentType) || DOCX_MIME_ALT.equalsIgnoreCase(contentType);
        };
    }

    private String generateStoredFileName(Long userId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userId + "_" + timestamp + "_" + random + "." + extension.toLowerCase(Locale.ROOT);
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
