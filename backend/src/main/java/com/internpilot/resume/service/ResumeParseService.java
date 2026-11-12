package com.internpilot.resume.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeParseService {

    String parse(MultipartFile file, String fileType);
}
