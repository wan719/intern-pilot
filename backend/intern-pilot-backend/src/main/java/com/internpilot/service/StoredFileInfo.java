package com.internpilot.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredFileInfo {

    private String originalFileName;

    private String storedFileName;

    private String filePath;

    private String fileType;

    private Long fileSize;
}
