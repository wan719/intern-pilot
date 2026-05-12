package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.interview.AiInterviewQuestionResult;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.InterviewQuestion;
import com.internpilot.entity.InterviewQuestionReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.enums.InterviewQuestionTypeEnum;
import com.internpilot.enums.QuestionDifficultyEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.InterviewQuestionMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.service.AiClient;
import com.internpilot.service.InterviewQuestionService;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.PromptUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionItemResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private final ResumeMapper resumeMapper;
    private final ResumeVersionMapper resumeVersionMapper;

    private final JobDescriptionMapper jobDescriptionMapper;

    private final AnalysisReportMapper analysisReportMapper;

    private final InterviewQuestionReportMapper interviewQuestionReportMapper;

    private final InterviewQuestionMapper interviewQuestionMapper;

    private final AiClient aiClient;

    private final AiProperties aiProperties;

    @Override
    @Transactional
    public InterviewQuestionGenerateResponse generate(InterviewQuestionGenerateRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Resume resume = getUserResumeOrThrow(request.getResumeId(), currentUserId);
        ResumeVersion resumeVersion = resolveResumeVersion(resume, request.getResumeVersionId(), currentUserId);
        String resumeText = resumeVersion == null ? resume.getParsedText() : resumeVersion.getContent();
        JobDescription job = getUserJobOrThrow(request.getJobId(), currentUserId);

        AnalysisReport analysisReport = null;
        if (request.getAnalysisReportId() != null) {
            analysisReport = getUserAnalysisReportOrThrow(request.getAnalysisReportId(), currentUserId);
            validateAnalysisReportScope(analysisReport, request.getResumeId(), request.getResumeVersionId(), request.getJobId());
        }

        if (!Boolean.TRUE.equals(request.getForceRefresh())) {
            InterviewQuestionReport existing = findExistingReport(
                    currentUserId,
                    request.getResumeId(),
                    resumeVersion == null ? null : resumeVersion.getId(),
                    request.getJobId(),
                    request.getAnalysisReportId()
            );

            if (existing != null) {
                return toGenerateResponse(existing, true);
            }
        }

        if (!StringUtils.hasText(resumeText)) {
            throw new BusinessException("简历解析文本为空，无法生成面试题");
        }

        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位JD为空，无法生成面试题");
        }

        String analysisReportText = buildAnalysisReportText(analysisReport);

        String prompt = PromptUtils.buildInterviewQuestionPrompt(
                resumeText,
                job.getJdContent(),
                analysisReportText
        );

        String rawResponse = aiClient.chat(prompt);

        AiInterviewQuestionResult aiResult = JsonUtils.parseAiJson(
                rawResponse,
                AiInterviewQuestionResult.class
        );

        if (aiResult.getQuestions() == null || aiResult.getQuestions().isEmpty()) {
            throw new BusinessException("AI 未生成有效面试题");
        }

        String title = StringUtils.hasText(aiResult.getTitle())
                ? aiResult.getTitle()
                : job.getCompanyName() + " " + job.getJobTitle() + " 面试题准备";

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setUserId(currentUserId);
        report.setResumeId(resume.getId());
        report.setResumeVersionId(resumeVersion == null ? null : resumeVersion.getId());
        report.setJobId(job.getId());
        report.setAnalysisReportId(request.getAnalysisReportId());
        report.setTitle(title);
        report.setQuestionCount(aiResult.getQuestions().size());
        report.setAiProvider(aiProperties.getProvider());
        report.setAiModel(aiProperties.getModel());
        report.setRawAiResponse(rawResponse);

        interviewQuestionReportMapper.insert(report);

        int sortOrder = 1;
        for (AiInterviewQuestionResult.QuestionItem item : aiResult.getQuestions()) {
            InterviewQuestion question = new InterviewQuestion();
            question.setReportId(report.getId());
            question.setUserId(currentUserId);
            question.setQuestionType(normalizeQuestionType(item.getQuestionType()));
            question.setDifficulty(normalizeDifficulty(item.getDifficulty()));
            question.setQuestion(item.getQuestion());
            question.setAnswer(item.getAnswer());
            question.setAnswerPoints(JsonUtils.toJsonString(nullToEmpty(item.getAnswerPoints())));
            question.setRelatedSkills(JsonUtils.toJsonString(nullToEmpty(item.getRelatedSkills())));
            question.setSortOrder(sortOrder++);

            interviewQuestionMapper.insert(question);
        }

        return toGenerateResponse(report, false);
    }

    @Override
    public PageResult<InterviewQuestionListResponse> list(
            Long resumeId,
            Long jobId,
            Integer pageNum,
            Integer pageSize
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<InterviewQuestionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestionReport::getUserId, currentUserId)
                .eq(InterviewQuestionReport::getDeleted, 0);

        if (resumeId != null) {
            wrapper.eq(InterviewQuestionReport::getResumeId, resumeId);
        }

        if (jobId != null) {
            wrapper.eq(InterviewQuestionReport::getJobId, jobId);
        }

        wrapper.orderByDesc(InterviewQuestionReport::getCreatedAt);

        Page<InterviewQuestionReport> page = new Page<>(pageNum, pageSize);
        Page<InterviewQuestionReport> resultPage = interviewQuestionReportMapper.selectPage(page, wrapper);

        List<InterviewQuestionListResponse> records = resultPage.getRecords()
                .stream()
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
    public InterviewQuestionDetailResponse getDetail(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport report = getUserQuestionReportOrThrow(reportId, currentUserId);

        List<InterviewQuestion> questions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, report.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0)
                        .orderByAsc(InterviewQuestion::getSortOrder)
        );

        return toDetailResponse(report, questions);
    }

    @Override
    @Transactional
    public Boolean delete(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport report = getUserQuestionReportOrThrow(reportId, currentUserId);

        report.setDeleted(1);
        interviewQuestionReportMapper.updateById(report);

        List<InterviewQuestion> questions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, report.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0)
        );

        for (InterviewQuestion question : questions) {
            question.setDeleted(1);
            interviewQuestionMapper.updateById(question);
        }

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

    private AnalysisReport getUserAnalysisReportOrThrow(Long reportId, Long userId) {
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

    private void validateAnalysisReportScope(AnalysisReport report, Long resumeId, Long resumeVersionId, Long jobId) {
        if (!Objects.equals(report.getResumeId(), resumeId) || !Objects.equals(report.getJobId(), jobId)) {
            throw new BusinessException("分析报告与所选简历或岗位不匹配");
        }
        if (resumeVersionId != null && report.getResumeVersionId() != null
                && !Objects.equals(report.getResumeVersionId(), resumeVersionId)) {
            throw new BusinessException("分析报告与所选简历版本不匹配");
        }
    }

    private InterviewQuestionReport getUserQuestionReportOrThrow(Long reportId, Long userId) {
        InterviewQuestionReport report = interviewQuestionReportMapper.selectOne(
                new LambdaQueryWrapper<InterviewQuestionReport>()
                        .eq(InterviewQuestionReport::getId, reportId)
                        .eq(InterviewQuestionReport::getUserId, userId)
                        .eq(InterviewQuestionReport::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (report == null) {
            throw new BusinessException("面试题报告不存在或无权限访问");
        }

        return report;
    }

    private InterviewQuestionReport findExistingReport(
            Long userId,
            Long resumeId,
            Long resumeVersionId,
            Long jobId,
            Long analysisReportId
    ) {
        LambdaQueryWrapper<InterviewQuestionReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQuestionReport::getUserId, userId)
                .eq(InterviewQuestionReport::getResumeId, resumeId)
                .eq(InterviewQuestionReport::getJobId, jobId)
                .eq(InterviewQuestionReport::getDeleted, 0);

        if (resumeVersionId != null) {
            wrapper.eq(InterviewQuestionReport::getResumeVersionId, resumeVersionId);
        } else {
            wrapper.isNull(InterviewQuestionReport::getResumeVersionId);
        }

        if (analysisReportId != null) {
            wrapper.eq(InterviewQuestionReport::getAnalysisReportId, analysisReportId);
        } else {
            wrapper.isNull(InterviewQuestionReport::getAnalysisReportId);
        }

        wrapper.orderByDesc(InterviewQuestionReport::getCreatedAt)
                .last("LIMIT 1");

        return interviewQuestionReportMapper.selectOne(wrapper);
    }

    private String buildAnalysisReportText(AnalysisReport report) {
        if (report == null) {
            return "暂无 AI 匹配分析报告，请主要根据简历和岗位 JD 生成面试题。";
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

    private String normalizeQuestionType(String type) {
        if (InterviewQuestionTypeEnum.isValid(type)) {
            return type;
        }
        return InterviewQuestionTypeEnum.JOB_SKILL.getCode();
    }

    private String normalizeDifficulty(String difficulty) {
        if (QuestionDifficultyEnum.isValid(difficulty)) {
            return difficulty;
        }
        return QuestionDifficultyEnum.MEDIUM.getCode();
    }

    private List<String> nullToEmpty(List<String> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private InterviewQuestionGenerateResponse toGenerateResponse(InterviewQuestionReport report, Boolean cacheHit) {
        InterviewQuestionGenerateResponse response = new InterviewQuestionGenerateResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setAnalysisReportId(report.getAnalysisReportId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCacheHit(cacheHit);
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private InterviewQuestionListResponse toListResponse(InterviewQuestionReport report) {
        InterviewQuestionListResponse response = new InterviewQuestionListResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        return response;
    }

    private InterviewQuestionDetailResponse toDetailResponse(
            InterviewQuestionReport report,
            List<InterviewQuestion> questions
    ) {
        InterviewQuestionDetailResponse response = new InterviewQuestionDetailResponse();
        response.setReportId(report.getId());
        response.setTitle(report.getTitle());
        response.setResumeId(report.getResumeId());
        response.setResumeVersionId(report.getResumeVersionId());
        response.setJobId(report.getJobId());
        response.setAnalysisReportId(report.getAnalysisReportId());
        response.setQuestionCount(report.getQuestionCount());
        response.setCreatedAt(report.getCreatedAt());

        JobDescription job = jobDescriptionMapper.selectById(report.getJobId());
        if (job != null) {
            response.setCompanyName(job.getCompanyName());
            response.setJobTitle(job.getJobTitle());
        }

        List<InterviewQuestionItemResponse> items = questions.stream()
                .filter(Objects::nonNull)
                .map(this::toQuestionItemResponse)
                .toList();

        response.setQuestions(items);

        return response;
    }

    private InterviewQuestionItemResponse toQuestionItemResponse(InterviewQuestion question) {
        InterviewQuestionItemResponse response = new InterviewQuestionItemResponse();
        response.setQuestionId(question.getId());
        response.setQuestionType(question.getQuestionType());
        response.setDifficulty(question.getDifficulty());
        response.setQuestion(question.getQuestion());
        response.setAnswer(question.getAnswer());
        response.setAnswerPoints(JsonUtils.fromJsonString(question.getAnswerPoints(), List.class));
        response.setRelatedSkills(JsonUtils.fromJsonString(question.getRelatedSkills(), List.class));
        response.setSortOrder(question.getSortOrder());
        return response;
    }

    private ResumeVersion resolveResumeVersion(Resume resume, Long versionId, Long userId) {
        if (versionId != null) {
            return getUserResumeVersionOrThrow(resume.getId(), versionId, userId);
        }

        return resumeVersionMapper.selectOne(
                new LambdaQueryWrapper<ResumeVersion>()
                        .eq(ResumeVersion::getResumeId, resume.getId())
                        .eq(ResumeVersion::getUserId, userId)
                        .eq(ResumeVersion::getIsCurrent, 1)
                        .eq(ResumeVersion::getDeleted, 0)
                        .orderByDesc(ResumeVersion::getUpdatedAt)
                        .last("LIMIT 1")
        );
    }

    private ResumeVersion getUserResumeVersionOrThrow(Long resumeId, Long versionId, Long userId) {
        ResumeVersion version = resumeVersionMapper.selectOne(
                new LambdaQueryWrapper<ResumeVersion>()
                        .eq(ResumeVersion::getId, versionId)
                        .eq(ResumeVersion::getResumeId, resumeId)
                        .eq(ResumeVersion::getUserId, userId)
                        .eq(ResumeVersion::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (version == null) {
            throw new BusinessException("简历版本不存在或无权限访问");
        }
        return version;
    }
}
