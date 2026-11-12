package com.internpilot.resume.service;

import com.internpilot.common.PageResult;
import com.internpilot.resume.vo.ResumeDetailResponse;
import com.internpilot.resume.vo.ResumeListResponse;
import com.internpilot.resume.vo.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeUploadResponse upload(MultipartFile file, String resumeName);

    PageResult<ResumeListResponse> list(Integer pageNum, Integer pageSize);

    ResumeDetailResponse getDetail(Long id);

    Boolean delete(Long id);

    Boolean setDefault(Long id);
}
