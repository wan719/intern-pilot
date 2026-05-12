package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.ApplicationRecord;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.JobRecommendationBatch;
import com.internpilot.entity.JobRecommendationItem;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.enums.RecommendationLevelEnum;
import com.internpilot.enums.RecommendationStrategyEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.JobRecommendationBatchMapper;
import com.internpilot.mapper.JobRecommendationItemMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.service.JobRecommendationService;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.RecommendationScoreCalculator;
import com.internpilot.util.SecurityUtils;
import com.internpilot.util.SkillKeywordUtils;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;
import com.internpilot.vo.recommendation.JobRecommendationItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRecommendationServiceImpl implements JobRecommendationService {

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final AnalysisReportMapper analysisReportMapper;
    private final ApplicationRecordMapper applicationRecordMapper;
    private final JobRecommendationBatchMapper batchMapper;
    private final JobRecommendationItemMapper itemMapper;

    @Override
    @Transactional
    public JobRecommendationGenerateResponse generate(JobRecommendationGenerateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(request.getResumeId(), currentUserId);
        ResumeVersion version = resolveResumeVersion(request.getResumeId(), request.getResumeVersionId(), currentUserId);
        String resumeText = resolveResumeText(resume, version);
        List<String> resumeSkills = SkillKeywordUtils.extractSkills(resumeText);

        List<JobDescription> jobs = jobDescriptionMapper.selectList(new LambdaQueryWrapper<JobDescription>()
                .eq(JobDescription::getUserId, currentUserId)
                .eq(JobDescription::getDeleted, 0)
                .orderByDesc(JobDescription::getCreatedAt)
                .orderByDesc(JobDescription::getId));
        if (jobs.isEmpty()) {
            throw new BusinessException("请先添加岗位，再生成岗位推荐");
        }

        JobRecommendationBatch batch = new JobRecommendationBatch();
        batch.setUserId(currentUserId);
        batch.setResumeId(resume.getId());
        batch.setResumeVersionId(version == null ? null : version.getId());
        batch.setTitle(buildBatchTitle(resume, version));
        batch.setJobCount(jobs.size());
        batch.setRecommendedCount(0);
        batch.setStrategy(RecommendationStrategyEnum.RULE_BASED.getCode());
        batchMapper.insert(batch);

        List<JobRecommendationItem> items = jobs.stream()
                .map(job -> buildRecommendationItem(job, batch.getId(), currentUserId, resume.getId(), version, resumeText, resumeSkills))
                .filter(item -> Boolean.TRUE.equals(request.getIncludeApplied()) || !Integer.valueOf(1).equals(item.getIsApplied()))
                .sorted(Comparator.comparing(JobRecommendationItem::getRecommendationScore).reversed()
                        .thenComparing(JobRecommendationItem::getJobId))
                .limit(normalizeLimit(request.getLimit()))
                .toList();

        for (int i = 0; i < items.size(); i++) {
            JobRecommendationItem item = items.get(i);
            item.setSortOrder(i + 1);
            itemMapper.insert(item);
        }

        batch.setRecommendedCount(items.size());
        batchMapper.updateById(batch);
        return toGenerateResponse(batch);
    }

    @Override
    public PageResult<JobRecommendationBatchListResponse> list(Integer pageNum, Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<JobRecommendationBatch> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<JobRecommendationBatch> resultPage = batchMapper.selectPage(page, new LambdaQueryWrapper<JobRecommendationBatch>()
                .eq(JobRecommendationBatch::getUserId, currentUserId)
                .eq(JobRecommendationBatch::getDeleted, 0)
                .orderByDesc(JobRecommendationBatch::getCreatedAt)
                .orderByDesc(JobRecommendationBatch::getId));

        List<JobRecommendationBatchListResponse> records = resultPage.getRecords().stream()
                .map(this::toListResponse)
                .toList();
        return new PageResult<>(records, resultPage.getTotal(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getPages());
    }

    @Override
    public JobRecommendationBatchDetailResponse getDetail(Long batchId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobRecommendationBatch batch = getUserBatchOrThrow(batchId, currentUserId);
        List<JobRecommendationItemResponse> items = itemMapper.selectList(new LambdaQueryWrapper<JobRecommendationItem>()
                        .eq(JobRecommendationItem::getBatchId, batch.getId())
                        .eq(JobRecommendationItem::getUserId, currentUserId)
                        .eq(JobRecommendationItem::getDeleted, 0)
                        .orderByAsc(JobRecommendationItem::getSortOrder)
                        .orderByDesc(JobRecommendationItem::getRecommendationScore))
                .stream()
                .map(this::toItemResponse)
                .toList();

        JobRecommendationBatchDetailResponse response = new JobRecommendationBatchDetailResponse();
        response.setBatchId(batch.getId());
        response.setResumeId(batch.getResumeId());
        response.setResumeVersionId(batch.getResumeVersionId());
        response.setTitle(batch.getTitle());
        response.setJobCount(batch.getJobCount());
        response.setRecommendedCount(batch.getRecommendedCount());
        response.setStrategy(batch.getStrategy());
        response.setCreatedAt(batch.getCreatedAt());
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional
    public Boolean delete(Long batchId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        JobRecommendationBatch batch = getUserBatchOrThrow(batchId, currentUserId);
        itemMapper.delete(new LambdaQueryWrapper<JobRecommendationItem>()
                .eq(JobRecommendationItem::getBatchId, batch.getId())
                .eq(JobRecommendationItem::getUserId, currentUserId));
        batchMapper.deleteById(batch.getId());
        return true;
    }

    private JobRecommendationItem buildRecommendationItem(
            JobDescription job,
            Long batchId,
            Long userId,
            Long resumeId,
            ResumeVersion version,
            String resumeText,
            List<String> resumeSkills
    ) {
        String jobText = joinText(job.getJobTitle(), job.getJobType(), job.getJdContent(), job.getSkillRequirements());
        List<String> jobSkills = SkillKeywordUtils.extractSkills(jobText);
        List<String> matchedSkills = SkillKeywordUtils.matchedSkills(resumeSkills, jobSkills);
        List<String> missingSkills = SkillKeywordUtils.missingSkills(resumeSkills, jobSkills);

        AnalysisReport report = findLatestReport(userId, resumeId, version == null ? null : version.getId(), job.getId());
        Integer skillScore = SkillKeywordUtils.calculateSkillScore(matchedSkills, jobSkills);
        Integer aiScore = report == null ? null : report.getMatchScore();
        Integer jobTypeScore = calculateJobTypeScore(resumeText, job);
        int finalScore = RecommendationScoreCalculator.calculateFinalScore(skillScore, aiScore, jobTypeScore);

        JobRecommendationItem item = new JobRecommendationItem();
        item.setBatchId(batchId);
        item.setUserId(userId);
        item.setJobId(job.getId());
        item.setAnalysisReportId(report == null ? null : report.getId());
        item.setRecommendationScore(finalScore);
        item.setRecommendationLevel(RecommendationLevelEnum.ofScore(finalScore));
        item.setSkillMatchScore(skillScore);
        item.setAiMatchScore(aiScore);
        item.setJobTypeScore(jobTypeScore);
        item.setMatchedSkills(JsonUtils.toJsonString(matchedSkills));
        item.setMissingSkills(JsonUtils.toJsonString(missingSkills));
        item.setReasons(JsonUtils.toJsonString(buildReasons(matchedSkills, missingSkills, aiScore, finalScore, job)));
        item.setIsApplied(hasApplied(userId, job.getId()) ? 1 : 0);
        item.setSortOrder(0);
        return item;
    }

    private List<String> buildReasons(
            List<String> matchedSkills,
            List<String> missingSkills,
            Integer aiScore,
            int finalScore,
            JobDescription job
    ) {
        List<String> reasons = new ArrayList<>();
        if (!matchedSkills.isEmpty()) {
            reasons.add("命中岗位要求技能：" + String.join("、", matchedSkills));
        }
        if (aiScore != null) {
            reasons.add("已复用历史 AI 匹配分：" + aiScore);
        }
        if (StringUtils.hasText(job.getJobType())) {
            reasons.add("岗位方向为：" + job.getJobType());
        }
        if (!missingSkills.isEmpty()) {
            reasons.add("需要补齐：" + String.join("、", missingSkills));
        }
        if (reasons.isEmpty()) {
            reasons.add(finalScore >= 60 ? "岗位基础信息与简历方向较接近" : "岗位要求与当前简历匹配度一般");
        }
        return reasons;
    }

    private AnalysisReport findLatestReport(Long userId, Long resumeId, Long resumeVersionId, Long jobId) {
        LambdaQueryWrapper<AnalysisReport> wrapper = new LambdaQueryWrapper<AnalysisReport>()
                .eq(AnalysisReport::getUserId, userId)
                .eq(AnalysisReport::getResumeId, resumeId)
                .eq(AnalysisReport::getJobId, jobId)
                .eq(AnalysisReport::getDeleted, 0)
                .orderByDesc(AnalysisReport::getCreatedAt)
                .orderByDesc(AnalysisReport::getId)
                .last("LIMIT 1");
        if (resumeVersionId != null) {
            wrapper.eq(AnalysisReport::getResumeVersionId, resumeVersionId);
        }
        return analysisReportMapper.selectOne(wrapper);
    }

    private boolean hasApplied(Long userId, Long jobId) {
        Long count = applicationRecordMapper.selectCount(new LambdaQueryWrapper<ApplicationRecord>()
                .eq(ApplicationRecord::getUserId, userId)
                .eq(ApplicationRecord::getJobId, jobId)
                .eq(ApplicationRecord::getDeleted, 0));
        return count != null && count > 0;
    }

    private Integer calculateJobTypeScore(String resumeText, JobDescription job) {
        String text = resumeText == null ? "" : resumeText.toLowerCase();
        if (StringUtils.hasText(job.getJobType()) && text.contains(job.getJobType().toLowerCase())) {
            return 100;
        }
        if (StringUtils.hasText(job.getJobTitle()) && text.contains(job.getJobTitle().toLowerCase())) {
            return 90;
        }
        return 60;
    }

    private ResumeVersion resolveResumeVersion(Long resumeId, Long resumeVersionId, Long userId) {
        if (resumeVersionId != null) {
            return getUserVersionOrThrow(resumeId, resumeVersionId, userId);
        }
        return resumeVersionMapper.selectOne(new LambdaQueryWrapper<ResumeVersion>()
                .eq(ResumeVersion::getResumeId, resumeId)
                .eq(ResumeVersion::getUserId, userId)
                .eq(ResumeVersion::getIsCurrent, 1)
                .eq(ResumeVersion::getDeleted, 0)
                .orderByDesc(ResumeVersion::getUpdatedAt)
                .orderByDesc(ResumeVersion::getId)
                .last("LIMIT 1"));
    }

    private String resolveResumeText(Resume resume, ResumeVersion version) {
        if (version != null && StringUtils.hasText(version.getContent())) {
            return version.getContent();
        }
        if (StringUtils.hasText(resume.getParsedText())) {
            return resume.getParsedText();
        }
        throw new BusinessException("简历内容为空，请先上传并解析简历，或创建简历版本");
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

    private JobRecommendationBatch getUserBatchOrThrow(Long batchId, Long userId) {
        JobRecommendationBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<JobRecommendationBatch>()
                .eq(JobRecommendationBatch::getId, batchId)
                .eq(JobRecommendationBatch::getUserId, userId)
                .eq(JobRecommendationBatch::getDeleted, 0)
                .last("LIMIT 1"));
        if (batch == null) {
            throw new BusinessException("推荐记录不存在或无权限访问");
        }
        return batch;
    }

    private JobRecommendationGenerateResponse toGenerateResponse(JobRecommendationBatch batch) {
        JobRecommendationGenerateResponse response = new JobRecommendationGenerateResponse();
        response.setBatchId(batch.getId());
        response.setResumeId(batch.getResumeId());
        response.setResumeVersionId(batch.getResumeVersionId());
        response.setTitle(batch.getTitle());
        response.setJobCount(batch.getJobCount());
        response.setRecommendedCount(batch.getRecommendedCount());
        response.setStrategy(batch.getStrategy());
        response.setCreatedAt(batch.getCreatedAt());
        return response;
    }

    private JobRecommendationBatchListResponse toListResponse(JobRecommendationBatch batch) {
        JobRecommendationBatchListResponse response = new JobRecommendationBatchListResponse();
        response.setBatchId(batch.getId());
        response.setResumeId(batch.getResumeId());
        response.setResumeVersionId(batch.getResumeVersionId());
        response.setTitle(batch.getTitle());
        response.setJobCount(batch.getJobCount());
        response.setRecommendedCount(batch.getRecommendedCount());
        response.setStrategy(batch.getStrategy());
        response.setCreatedAt(batch.getCreatedAt());
        return response;
    }

    private JobRecommendationItemResponse toItemResponse(JobRecommendationItem item) {
        JobDescription job = jobDescriptionMapper.selectById(item.getJobId());
        JobRecommendationItemResponse response = new JobRecommendationItemResponse();
        response.setItemId(item.getId());
        response.setJobId(item.getJobId());
        response.setAnalysisReportId(item.getAnalysisReportId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
            response.setJobType(job.getJobType());
            response.setLocation(job.getLocation());
            response.setSalaryRange(job.getSalaryRange());
            response.setSourcePlatform(job.getSourcePlatform());
        }
        response.setRecommendationScore(item.getRecommendationScore());
        response.setRecommendationLevel(item.getRecommendationLevel());
        response.setSkillMatchScore(item.getSkillMatchScore());
        response.setAiMatchScore(item.getAiMatchScore());
        response.setJobTypeScore(item.getJobTypeScore());
        response.setMatchedSkills(JsonUtils.toStringList(item.getMatchedSkills()));
        response.setMissingSkills(JsonUtils.toStringList(item.getMissingSkills()));
        response.setReasons(JsonUtils.toStringList(item.getReasons()));
        response.setIsApplied(item.getIsApplied());
        response.setSortOrder(item.getSortOrder());
        return response;
    }

    private String buildBatchTitle(Resume resume, ResumeVersion version) {
        String name = version != null && StringUtils.hasText(version.getVersionName())
                ? version.getVersionName()
                : resume.getResumeName();
        if (!StringUtils.hasText(name)) {
            name = resume.getOriginalFileName();
        }
        return "基于 " + name + " 的岗位推荐";
    }

    private String joinText(String... parts) {
        return String.join(" ", Arrays.stream(parts).filter(StringUtils::hasText).toList());
    }

    private long normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10L;
        }
        return Math.min(limit, 50);
    }

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }
}
