package com.internpilot.resume.service;

import com.internpilot.resume.enums.FileTypeEnum;

import java.nio.file.Path;

public record ResumeOriginalFile(Path path, FileTypeEnum type, String originalName) {
}
