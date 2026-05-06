package com.internpilot.service.impl;

import com.internpilot.config.FileStorageConfig;
import com.internpilot.exception.FileParseException;
import com.internpilot.service.StoredFileInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStorePdfFile() {
        FileStorageConfig config = new FileStorageConfig();
        config.setUploadDir(tempDir.toString());
        config.setMaxSize(1024L * 1024L);
        config.setAllowedTypes(List.of("pdf", "docx"));

        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(config);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "mock pdf content".getBytes()
        );

        StoredFileInfo info = service.store(file, 1L);

        assertEquals("resume.pdf", info.getOriginalFileName());
        assertEquals("PDF", info.getFileType());
        assertTrue(info.getStoredFileName().endsWith(".pdf"));
        assertTrue(Path.of(info.getFilePath()).toFile().exists());
    }

    @Test
    void shouldRejectUnsupportedFileType() {
        FileStorageConfig config = new FileStorageConfig();
        config.setUploadDir(tempDir.toString());

        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(config);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.txt",
                "text/plain",
                "hello".getBytes()
        );

        FileParseException exception = assertThrows(FileParseException.class, () -> service.store(file, 1L));
        assertNotNull(exception.getMessage());
    }

    @Test
    void shouldRejectTooLargeFile() {
        FileStorageConfig config = new FileStorageConfig();
        config.setUploadDir(tempDir.toString());
        config.setMaxSize(4L);

        LocalFileStorageServiceImpl service = new LocalFileStorageServiceImpl(config);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "12345".getBytes()
        );

        assertThrows(FileParseException.class, () -> service.store(file, 1L));
    }
}
