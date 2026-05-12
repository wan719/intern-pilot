package com.internpilot.service;

import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;

import java.util.List;

public interface ResumeVersionService {

    ResumeVersionCreateResponse create(Long resumeId, ResumeVersionCreateRequest request);

    List<ResumeVersionListResponse> list(Long resumeId);

    ResumeVersionDetailResponse getDetail(Long resumeId, Long versionId);

    Boolean update(Long resumeId, Long versionId, ResumeVersionUpdateRequest request);

    Boolean setCurrent(Long resumeId, Long versionId);

    Boolean delete(Long resumeId, Long versionId);

    ResumeVersionCreateResponse optimize(Long resumeId, ResumeVersionOptimizeRequest request);

    ResumeVersionCompareResponse compare(Long resumeId, Long oldVersionId, Long newVersionId);
}