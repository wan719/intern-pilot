package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.Resume;
import com.internpilot.enums.ParseStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.FileStorageService;
import com.internpilot.service.ResumeParseService;
import com.internpilot.service.ResumeService;
import com.internpilot.service.StoredFileInfo;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final FileStorageService fileStorageService;
    private final ResumeParseService resumeParseService;

    @Override
    @Transactional
    public ResumeUploadResponse upload(MultipartFile file, String resumeName) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        StoredFileInfo fileInfo = fileStorageService.store(file, currentUserId);

        try {
            String parsedText = resumeParseService.parse(file, fileInfo.getFileType());
            Long resumeCount = resumeMapper.selectCount(new LambdaQueryWrapper<Resume>()
                    .eq(Resume::getUserId, currentUserId)
                    .eq(Resume::getDeleted, 0));

            boolean isFirstResume = resumeCount == null || resumeCount == 0;

            Resume resume = new Resume();
            resume.setUserId(currentUserId);
            resume.setResumeName(resumeName);
            resume.setOriginalFileName(fileInfo.getOriginalFileName());
            resume.setStoredFileName(fileInfo.getStoredFileName());
            resume.setFilePath(fileInfo.getFilePath());
            resume.setFileType(fileInfo.getFileType());
            resume.setFileSize(fileInfo.getFileSize());
            resume.setParsedText(parsedText);
            resume.setParseStatus(ParseStatusEnum.SUCCESS.getCode());
            resume.setIsDefault(isFirstResume ? 1 : 0);

            resumeMapper.insert(resume);
            return toUploadResponse(resume);
        } catch (RuntimeException e) {
            deleteStoredFileQuietly(fileInfo.getFilePath());
            throw e;
        }
    }

    @Override
    public PageResult<ResumeListResponse> list(Integer pageNum, Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Page<Resume> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<Resume> resultPage = resumeMapper.selectPage(
                page,
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
                        .orderByDesc(Resume::getCreatedAt)
        );

        List<ResumeListResponse> records = resultPage.getRecords().stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public ResumeDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);
        return toDetailResponse(resume);
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);
        resume.setIsDefault(0);
        resumeMapper.updateById(resume);
        resumeMapper.deleteById(resume.getId());
        return true;
    }

    @Override
    @Transactional
    public Boolean setDefault(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);

        Resume clearDefault = new Resume();
        clearDefault.setIsDefault(0);
        resumeMapper.update(
                clearDefault,
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
        );

        Resume target = new Resume();
        target.setId(resume.getId());
        target.setIsDefault(1);
        resumeMapper.updateById(target);
        return true;
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (resume == null) {
            throw new BusinessException("简历不存在或无权限访问");
        }
        return resume;
    }

    private ResumeUploadResponse toUploadResponse(Resume resume) {
        ResumeUploadResponse response = new ResumeUploadResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setParsedTextPreview(buildPreview(resume.getParsedText()));
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setCreatedAt(resume.getCreatedAt());
        return response;
    }

    private ResumeListResponse toListResponse(Resume resume) {
        ResumeListResponse response = new ResumeListResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setCreatedAt(resume.getCreatedAt());
        return response;
    }

    private ResumeDetailResponse toDetailResponse(Resume resume) {
        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setStoredFileName(resume.getStoredFileName());
        response.setFilePath(resume.getFilePath());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setParsedText(resume.getParsedText());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    private String buildPreview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int maxLength = 100;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private void deleteStoredFileQuietly(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException ignored) {
            // Best effort cleanup. DB transaction still protects persistence state.
        }
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize.longValue();
    }
}
