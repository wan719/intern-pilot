package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.resume.ResumeVersionCreateRequest;
import com.internpilot.dto.resume.ResumeVersionOptimizeRequest;
import com.internpilot.dto.resume.ResumeVersionUpdateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.enums.ResumeVersionTypeEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.service.AiClient;
import com.internpilot.service.ResumeVersionService;
import com.internpilot.util.PromptUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResumeVersionServiceImpl implements ResumeVersionService {

    private static final int SUMMARY_LENGTH = 160;

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final AiClient aiClient;

    @Override
    @Transactional
    public ResumeVersionCreateResponse create(Long resumeId, ResumeVersionCreateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);

        String versionType = StringUtils.hasText(request.getVersionType())
                ? request.getVersionType().trim()
                : ResumeVersionTypeEnum.MANUAL.getCode();
        validateVersionType(versionType);

        if (request.getTargetJobId() != null) {
            getUserJobOrThrow(request.getTargetJobId(), currentUserId);
        }
        if (request.getSourceVersionId() != null) {
            getUserVersionOrThrow(resumeId, request.getSourceVersionId(), currentUserId);
        }

        ResumeVersion version = new ResumeVersion();
        version.setUserId(currentUserId);
        version.setResumeId(resumeId);
        version.setVersionName(request.getVersionName().trim());
        version.setVersionType(versionType);
        version.setContent(request.getContent());
        version.setContentSummary(buildSummary(request.getContent()));
        version.setTargetJobId(request.getTargetJobId());
        version.setSourceVersionId(request.getSourceVersionId());
        version.setIsCurrent(hasActiveVersion(resumeId, currentUserId) ? 0 : 1);

        resumeVersionMapper.insert(version);
        return toCreateResponse(version);
    }

    @Override
    public List<ResumeVersionListResponse> list(Long resumeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);

        return resumeVersionMapper.selectList(new LambdaQueryWrapper<ResumeVersion>()
                        .eq(ResumeVersion::getResumeId, resumeId)
                        .eq(ResumeVersion::getUserId, currentUserId)
                        .eq(ResumeVersion::getDeleted, 0)
                        .orderByDesc(ResumeVersion::getIsCurrent)
                        .orderByDesc(ResumeVersion::getCreatedAt)
                        .orderByDesc(ResumeVersion::getId))
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public ResumeVersionDetailResponse getDetail(Long resumeId, Long versionId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        return toDetailResponse(getUserVersionOrThrow(resumeId, versionId, currentUserId));
    }

    @Override
    @Transactional
    public Boolean update(Long resumeId, Long versionId, ResumeVersionUpdateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        ResumeVersion version = getUserVersionOrThrow(resumeId, versionId, currentUserId);

        version.setVersionName(request.getVersionName().trim());
        version.setContent(request.getContent());
        version.setContentSummary(buildSummary(request.getContent()));
        resumeVersionMapper.updateById(version);
        return true;
    }

    @Override
    @Transactional
    public Boolean setCurrent(Long resumeId, Long versionId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        ResumeVersion version = getUserVersionOrThrow(resumeId, versionId, currentUserId);

        ResumeVersion clear = new ResumeVersion();
        clear.setIsCurrent(0);
        resumeVersionMapper.update(clear, new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getUserId, currentUserId)
                .eq(ResumeVersion::getDeleted, 0));

        ResumeVersion target = new ResumeVersion();
        target.setId(version.getId());
        target.setIsCurrent(1);
        resumeVersionMapper.updateById(target);
        return true;
    }

    @Override
    @Transactional
    public Boolean delete(Long resumeId, Long versionId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        ResumeVersion version = getUserVersionOrThrow(resumeId, versionId, currentUserId);

        if (Integer.valueOf(1).equals(version.getIsCurrent())) {
            throw new BusinessException("当前版本不能删除，请先切换当前版本");
        }
        if (ResumeVersionTypeEnum.ORIGINAL.getCode().equals(version.getVersionType())) {
            throw new BusinessException("原始版本不能删除");
        }
        if (activeVersionCount(resumeId, currentUserId) <= 1) {
            throw new BusinessException("至少保留一个简历版本");
        }

        resumeVersionMapper.deleteById(version.getId());
        return true;
    }

    @Override
    @Transactional
    public ResumeVersionCreateResponse optimize(Long resumeId, ResumeVersionOptimizeRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        ResumeVersion sourceVersion = getUserVersionOrThrow(resumeId, request.getSourceVersionId(), currentUserId);
        JobDescription job = getUserJobOrThrow(request.getTargetJobId(), currentUserId);

        AnalysisReport analysisReport = null;
        if (request.getAiReportId() != null) {
            analysisReport = getUserAnalysisReportOrThrow(request.getAiReportId(), currentUserId);
            if (!resumeId.equals(analysisReport.getResumeId()) || !job.getId().equals(analysisReport.getJobId())) {
                throw new BusinessException("AI分析报告与所选简历或岗位不匹配");
            }
        }

        String prompt = PromptUtils.buildResumeOptimizePrompt(
                sourceVersion.getContent(),
                job.getJdContent(),
                buildAnalysisReportText(analysisReport),
                request.getExtraRequirement()
        );
        String optimizedContent = aiClient.chat(prompt);
        if (!StringUtils.hasText(optimizedContent)) {
            throw new BusinessException("AI未返回有效简历内容");
        }

        ResumeVersion version = new ResumeVersion();
        version.setUserId(currentUserId);
        version.setResumeId(resumeId);
        version.setVersionName(StringUtils.hasText(request.getVersionName())
                ? request.getVersionName().trim()
                : job.getCompanyName() + job.getJobTitle() + "定制版");
        version.setVersionType(ResumeVersionTypeEnum.AI_OPTIMIZED.getCode());
        version.setContent(optimizedContent);
        version.setContentSummary(buildSummary(optimizedContent));
        version.setTargetJobId(job.getId());
        version.setSourceVersionId(sourceVersion.getId());
        version.setAiReportId(request.getAiReportId());
        version.setOptimizePrompt(prompt);
        version.setAiRawResponse(optimizedContent);
        version.setIsCurrent(0);

        resumeVersionMapper.insert(version);
        return toCreateResponse(version);
    }

    @Override
    public ResumeVersionCompareResponse compare(Long resumeId, Long oldVersionId, Long newVersionId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        getUserResumeOrThrow(resumeId, currentUserId);
        ResumeVersion oldVersion = getUserVersionOrThrow(resumeId, oldVersionId, currentUserId);
        ResumeVersion newVersion = getUserVersionOrThrow(resumeId, newVersionId, currentUserId);

        List<String> oldLines = splitLines(oldVersion.getContent());
        List<String> newLines = splitLines(newVersion.getContent());
        Set<String> oldSet = new LinkedHashSet<>(oldLines);
        Set<String> newSet = new LinkedHashSet<>(newLines);

        List<String> added = newLines.stream().filter(line -> !oldSet.contains(line)).toList();
        List<String> removed = oldLines.stream().filter(line -> !newSet.contains(line)).toList();
        List<String> common = newLines.stream().filter(oldSet::contains).toList();

        ResumeVersionCompareResponse response = new ResumeVersionCompareResponse();
        response.setOldVersionId(oldVersion.getId());
        response.setOldVersionName(oldVersion.getVersionName());
        response.setNewVersionId(newVersion.getId());
        response.setNewVersionName(newVersion.getVersionName());
        response.setOldContent(oldVersion.getContent());
        response.setNewContent(newVersion.getContent());
        response.setAddedLines(added);
        response.setRemovedLines(removed);
        response.setCommonLines(common);
        response.setOldLength(oldVersion.getContent() == null ? 0 : oldVersion.getContent().length());
        response.setNewLength(newVersion.getContent() == null ? 0 : newVersion.getContent().length());
        response.setAddedCount(added.size());
        response.setRemovedCount(removed.size());
        return response;
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectOne(new LambdaQueryWrapper<Resume>()
                .eq(Resume::getId, resumeId)
                .eq(Resume::getUserId, userId)
                .eq(Resume::getDeleted, 0)
                .last("LIMIT 1"));
        if (resume == null) {
            throw new BusinessException("简历不存在或无权限访问");
        }
        return resume;
    }

    private ResumeVersion getUserVersionOrThrow(Long resumeId, Long versionId, Long userId) {
        ResumeVersion version = resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getId, versionId)
                .eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getUserId, userId)
                .eq(ResumeVersion::getDeleted, 0)
                .last("LIMIT 1"));
        if (version == null) {
            throw new BusinessException("简历版本不存在或无权限访问");
        }
        return version;
    }

    private JobDescription getUserJobOrThrow(Long jobId, Long userId) {
        JobDescription job = jobDescriptionMapper.selectOne(new LambdaQueryWrapper<JobDescription>()
                .eq(JobDescription::getId, jobId)
                .eq(JobDescription::getUserId, userId)
                .eq(JobDescription::getDeleted, 0)
                .last("LIMIT 1"));
        if (job == null) {
            throw new BusinessException("岗位不存在或无权限访问");
        }
        return job;
    }

    private AnalysisReport getUserAnalysisReportOrThrow(Long reportId, Long userId) {
        AnalysisReport report = analysisReportMapper.selectOne(new LambdaQueryWrapper<AnalysisReport>()
                .eq(AnalysisReport::getId, reportId)
                .eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getDeleted, 0)
                .last("LIMIT 1"));
        if (report == null) {
            throw new BusinessException("AI分析报告不存在或无权限访问");
        }
        return report;
    }

    private void validateVersionType(String versionType) {
        if (!ResumeVersionTypeEnum.isValid(versionType)) {
            throw new BusinessException("简历版本类型不合法");
        }
    }

    private boolean hasActiveVersion(Long resumeId, Long userId) {
        return activeVersionCount(resumeId, userId) > 0;
    }

    private long activeVersionCount(Long resumeId, Long userId) {
        Long count = resumeVersionMapper.selectCount(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getUserId, userId)
                .eq(ResumeVersion::getDeleted, 0));
        return count == null ? 0 : count;
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() <= SUMMARY_LENGTH ? compact : compact.substring(0, SUMMARY_LENGTH) + "...";
    }

    private String buildAnalysisReportText(AnalysisReport report) {
        if (report == null) {
            return "暂无 AI 匹配分析报告，请主要根据原始简历和岗位 JD 优化。";
        }
        return """
                匹配分数：%s
                匹配等级：%s
                简历优势：%s
                简历短板：%s
                缺失技能：%s
                优化建议：%s
                面试准备建议：%s
                """.formatted(
                report.getMatchScore(),
                report.getMatchLevel(),
                report.getStrengths(),
                report.getWeaknesses(),
                report.getMissingSkills(),
                report.getSuggestions(),
                report.getInterviewTips()
        );
    }

    private List<String> splitLines(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
    }

    private ResumeVersionCreateResponse toCreateResponse(ResumeVersion version) {
        ResumeVersionCreateResponse response = new ResumeVersionCreateResponse();
        response.setVersionId(version.getId());
        response.setResumeId(version.getResumeId());
        response.setVersionName(version.getVersionName());
        response.setVersionType(version.getVersionType());
        response.setIsCurrent(version.getIsCurrent());
        response.setCreatedAt(version.getCreatedAt());
        return response;
    }

    private ResumeVersionListResponse toListResponse(ResumeVersion version) {
        ResumeVersionListResponse response = new ResumeVersionListResponse();
        response.setVersionId(version.getId());
        response.setResumeId(version.getResumeId());
        response.setVersionName(version.getVersionName());
        response.setVersionType(version.getVersionType());
        response.setContentSummary(version.getContentSummary());
        response.setTargetJobId(version.getTargetJobId());
        response.setIsCurrent(version.getIsCurrent());
        response.setCreatedAt(version.getCreatedAt());
        response.setUpdatedAt(version.getUpdatedAt());
        fillJobInfo(response, version.getTargetJobId());
        return response;
    }

    private ResumeVersionDetailResponse toDetailResponse(ResumeVersion version) {
        ResumeVersionDetailResponse response = new ResumeVersionDetailResponse();
        response.setVersionId(version.getId());
        response.setResumeId(version.getResumeId());
        response.setVersionName(version.getVersionName());
        response.setVersionType(version.getVersionType());
        response.setContent(version.getContent());
        response.setContentSummary(version.getContentSummary());
        response.setTargetJobId(version.getTargetJobId());
        response.setSourceVersionId(version.getSourceVersionId());
        response.setAiReportId(version.getAiReportId());
        response.setIsCurrent(version.getIsCurrent());
        response.setCreatedAt(version.getCreatedAt());
        response.setUpdatedAt(version.getUpdatedAt());
        fillJobInfo(response, version.getTargetJobId());
        return response;
    }

    private void fillJobInfo(ResumeVersionListResponse response, Long targetJobId) {
        if (targetJobId == null) {
            return;
        }
        JobDescription job = jobDescriptionMapper.selectById(targetJobId);
        if (job != null && Integer.valueOf(0).equals(job.getDeleted())) {
            response.setTargetCompanyName(job.getCompanyName());
            response.setTargetJobTitle(job.getJobTitle());
        }
    }

    private void fillJobInfo(ResumeVersionDetailResponse response, Long targetJobId) {
        if (targetJobId == null) {
            return;
        }
        JobDescription job = jobDescriptionMapper.selectById(targetJobId);
        if (job != null && Integer.valueOf(0).equals(job.getDeleted())) {
            response.setTargetCompanyName(job.getCompanyName());
            response.setTargetJobTitle(job.getJobTitle());
        }
    }
}
