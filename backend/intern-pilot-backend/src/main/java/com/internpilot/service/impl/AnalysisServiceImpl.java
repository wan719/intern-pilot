package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.common.PageResult;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.analysis.AiAnalysisResult;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.enums.MatchLevelEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.service.AiClient;
import com.internpilot.service.AnalysisService;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.PromptUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private static final long CACHE_TTL_HOURS = 24;

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final AiClient aiClient;
    private final AiProperties aiProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AnalysisResultResponse match(AnalysisMatchRequest request) {
        return matchForUser(request, SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional
    public AnalysisResultResponse matchForUser(AnalysisMatchRequest request, Long userId) {
        Resume resume = getUserResumeOrThrow(request.getResumeId(), userId);
        JobDescription job = getUserJobOrThrow(request.getJobId(), userId);
        ResumeVersion version = resolveResumeVersion(resume, request.getResumeVersionId(), userId);
        String resumeText = version == null ? resume.getParsedText() : version.getContent();

        if (!StringUtils.hasText(resumeText)) {
            throw new BusinessException("简历解析文本为空");
        }
        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位 JD 不能为空");
        }

        Long resumeVersionId = version == null ? null : version.getId();
        String cacheKey = buildCacheKey(userId, resume.getId(), resumeVersionId, job.getId());
        boolean forceRefresh = Boolean.TRUE.equals(request.getForceRefresh());

        if (!forceRefresh) {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof AnalysisResultResponse cachedResponse) {
                AnalysisResultResponse copy = copyResultResponse(cachedResponse);
                copy.setCacheHit(true);
                return copy;
            }
            if (cached != null) {
                AnalysisResultResponse cachedResponse = objectMapper.convertValue(cached, AnalysisResultResponse.class);
                AnalysisResultResponse copy = copyResultResponse(cachedResponse);
                copy.setCacheHit(true);
                return copy;
            }
        }

        String prompt = PromptUtils.buildResumeJobMatchPrompt(resumeText, job.getJdContent());
        String rawResponse = aiClient.chat(prompt);
        AiAnalysisResult aiResult = JsonUtils.parseAiJson(rawResponse, AiAnalysisResult.class);
        normalizeAiResult(aiResult);

        AnalysisReport report = new AnalysisReport();
        report.setUserId(userId);
        report.setResumeId(resume.getId());
        report.setResumeVersionId(resumeVersionId);
        report.setJobId(job.getId());
        report.setMatchScore(aiResult.getMatchScore());
        report.setMatchLevel(aiResult.getMatchLevel());
        report.setStrengths(JsonUtils.toJsonString(nullToEmpty(aiResult.getStrengths())));
        report.setWeaknesses(JsonUtils.toJsonString(nullToEmpty(aiResult.getWeaknesses())));
        report.setMissingSkills(JsonUtils.toJsonString(nullToEmpty(aiResult.getMissingSkills())));
        report.setSuggestions(JsonUtils.toJsonString(nullToEmpty(aiResult.getSuggestions())));
        report.setInterviewTips(JsonUtils.toJsonString(nullToEmpty(aiResult.getInterviewTips())));
        report.setRawAiResponse(rawResponse);
        report.setAiProvider(aiProperties.getProvider());
        report.setAiModel(aiProperties.getModel());
        report.setCacheHit(0);

        analysisReportMapper.insert(report);

        AnalysisResultResponse response = toResultResponse(report);
        response.setCacheHit(false);

        redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(CACHE_TTL_HOURS));
        return response;
    }

    @Override
    public PageResult<AnalysisReportListResponse> listReports(
            Long resumeId,
            Long jobId,
            Integer minScore,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisReport::getUserId, currentUserId)
                .eq(AnalysisReport::getDeleted, 0);

        if (resumeId != null) {
            wrapper.eq(AnalysisReport::getResumeId, resumeId);
        }
        if (jobId != null) {
            wrapper.eq(AnalysisReport::getJobId, jobId);
        }
        if (minScore != null) {
            wrapper.ge(AnalysisReport::getMatchScore, minScore);
        }

        wrapper.orderByDesc(AnalysisReport::getCreatedAt);

        Page<AnalysisReport> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<AnalysisReport> resultPage = analysisReportMapper.selectPage(page, wrapper);

        List<AnalysisReportListResponse> records = resultPage.getRecords().stream()
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
    public AnalysisReportDetailResponse getReportDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AnalysisReport report = getUserReportOrThrow(id, currentUserId);
        return toDetailResponse(report);
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

    private JobDescription getUserJobOrThrow(Long jobId, Long userId) {
        JobDescription job = jobDescriptionMapper.selectOne(
                new LambdaQueryWrapper<JobDescription>()
                        .eq(JobDescription::getId, jobId)
                        .eq(JobDescription::getUserId, userId)
                        .eq(JobDescription::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (job == null) {
            throw new BusinessException("岗位不存在或无权限访问");
        }
        return job;
    }

    private AnalysisReport getUserReportOrThrow(Long reportId, Long userId) {
        AnalysisReport report = analysisReportMapper.selectOne(
                new LambdaQueryWrapper<AnalysisReport>()
                        .eq(AnalysisReport::getId, reportId)
                        .eq(AnalysisReport::getUserId, userId)
                        .eq(AnalysisReport::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (report == null) {
            throw new BusinessException("分析报告不存在或无权限访问");
        }
        return report;
    }

    private String buildCacheKey(Long userId, Long resumeId, Long resumeVersionId, Long jobId) {
        return "internpilot:analysis:%d:%d:%d:%d".formatted(userId, resumeId, resumeVersionId == null ? 0 : resumeVersionId, jobId);
    }

    private ResumeVersion resolveResumeVersion(Resume resume, Long versionId, Long userId) {
        if (versionId != null) {
            return getUserResumeVersionOrThrow(resume.getId(), versionId, userId);
        }

        ResumeVersion current = resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resume.getId())
                .eq(ResumeVersion::getUserId, userId)
                .eq(ResumeVersion::getIsCurrent, 1)
                .eq(ResumeVersion::getDeleted, 0)
                .orderByDesc(ResumeVersion::getUpdatedAt)
                .last("LIMIT 1"));

        if (current != null) {
            return current;
        }
        return null;
    }

    private ResumeVersion getUserResumeVersionOrThrow(Long resumeId, Long versionId, Long userId) {
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

    private void normalizeAiResult(AiAnalysisResult result) {
        if (result.getMatchScore() == null) {
            result.setMatchScore(60);
        }
        if (result.getMatchScore() < 0) {
            result.setMatchScore(0);
        }
        if (result.getMatchScore() > 100) {
            result.setMatchScore(100);
        }
        if (!StringUtils.hasText(result.getMatchLevel())) {
            result.setMatchLevel(MatchLevelEnum.fromScore(result.getMatchScore()));
        }
    }

    private List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private AnalysisResultResponse toResultResponse(AnalysisReport report) {
        AnalysisResultResponse response = new AnalysisResultResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setStrengths(JsonUtils.toStringList(report.getStrengths()));
        response.setWeaknesses(JsonUtils.toStringList(report.getWeaknesses()));
        response.setMissingSkills(JsonUtils.toStringList(report.getMissingSkills()));
        response.setSuggestions(JsonUtils.toStringList(report.getSuggestions()));
        response.setInterviewTips(JsonUtils.toStringList(report.getInterviewTips()));
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private AnalysisResultResponse copyResultResponse(AnalysisResultResponse source) {
        AnalysisResultResponse copy = new AnalysisResultResponse();
        copy.setReportId(source.getReportId());
        copy.setResumeId(source.getResumeId());
        copy.setResumeVersionId(source.getResumeVersionId());
        copy.setJobId(source.getJobId());
        copy.setMatchScore(source.getMatchScore());
        copy.setMatchLevel(source.getMatchLevel());
        copy.setStrengths(source.getStrengths());
        copy.setWeaknesses(source.getWeaknesses());
        copy.setMissingSkills(source.getMissingSkills());
        copy.setSuggestions(source.getSuggestions());
        copy.setInterviewTips(source.getInterviewTips());
        copy.setCacheHit(source.getCacheHit());
        copy.setCreatedAt(source.getCreatedAt());
        return copy;
    }

    private AnalysisReportListResponse toListResponse(AnalysisReport report) {
        AnalysisReportListResponse response = new AnalysisReportListResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }
        return response;
    }

    private AnalysisReportDetailResponse toDetailResponse(AnalysisReport report) {
        AnalysisReportDetailResponse response = new AnalysisReportDetailResponse();
        response.setReportId(report.getId());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setMatchScore(report.getMatchScore());
        response.setMatchLevel(report.getMatchLevel());
        response.setStrengths(JsonUtils.toStringList(report.getStrengths()));
        response.setWeaknesses(JsonUtils.toStringList(report.getWeaknesses()));
        response.setMissingSkills(JsonUtils.toStringList(report.getMissingSkills()));
        response.setSuggestions(JsonUtils.toStringList(report.getSuggestions()));
        response.setInterviewTips(JsonUtils.toStringList(report.getInterviewTips()));
        response.setAiProvider(report.getAiProvider());
        response.setAiModel(report.getAiModel());
        response.setCacheHit(report.getCacheHit() != null && report.getCacheHit() == 1);
        response.setCreatedAt(report.getCreatedAt());

        Resume resume = resumeMapper.selectById(report.getResumeId());
        if (resume != null) {
            response.setResumeName(resume.getResumeName());
        }

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }
        return response;
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize.longValue();
    }
}
