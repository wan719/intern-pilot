package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeUploadResponse upload(MultipartFile file, String resumeName);

    PageResult<ResumeListResponse> list(Integer pageNum, Integer pageSize);

    ResumeDetailResponse getDetail(Long id);

    Boolean delete(Long id);

    Boolean setDefault(Long id);
}
