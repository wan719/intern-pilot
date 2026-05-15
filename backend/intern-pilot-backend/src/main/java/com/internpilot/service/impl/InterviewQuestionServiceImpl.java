package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.interview.AiInterviewQuestionResult;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
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
import com.internpilot.service.RagKnowledgeService;
import com.internpilot.util.InterviewQuestionParser;
import com.internpilot.util.InterviewQuestionPromptBuilder;
import com.internpilot.util.JsonUtils;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.interview.InterviewQuestionItemResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
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
    private final RagKnowledgeService ragKnowledgeService;

    private static final int DEFAULT_QUESTION_COUNT = 8;
    private static final int MIN_QUESTION_COUNT = 3;
    private static final int MAX_QUESTION_COUNT = 20;
    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "JAVA_BASIC", "SPRING_BOOT", "SPRING_SECURITY",
            "MYSQL", "REDIS", "PROJECT", "HR", "RESUME", "JOB_SKILL"
    );
    private static final List<String> DEFAULT_DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

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
            validateAnalysisReportScope(analysisReport, request.getResumeId(), request.getResumeVersionId(),
                    request.getJobId());
        }

        if (!StringUtils.hasText(resumeText)) {
            throw new BusinessException("简历解析文本为空，无法生成面试题");
        }

        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位JD为空，无法生成面试题");
        }

        String analysisReportText = buildAnalysisReportText(analysisReport);
        String ragContext = buildRagContext(resumeText, job);
        if (StringUtils.hasText(ragContext)) {
            analysisReportText = analysisReportText + "\n\n【岗位知识库参考内容】\n" + ragContext;
        }

        String prompt = InterviewQuestionPromptBuilder.build(
                resumeText,
                job.getJdContent(),
                analysisReportText,
                request);

        String rawResponse = aiClient.chat(prompt);

        AiInterviewQuestionResult aiResult = parseInterviewQuestions(rawResponse, job, request);

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
        report.setTitle(ensureChineseTitle(title, job));
        report.setQuestionCount(aiResult.getQuestions().size());
        report.setAiProvider(aiProperties.getProvider());
        report.setAiModel(aiProperties.getModel());
        report.setRawAiResponse(rawResponse);

        interviewQuestionReportMapper.insert(report);
        if (report.getId() == null) {
            throw new BusinessException("面试题报告保存失败，请稍后重试");
        }

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
            question.setFollowUps(JsonUtils.toJsonString(nullToEmpty(item.getFollowUps())));
            question.setKeywords(JsonUtils.toJsonString(nullToEmpty(item.getKeywords())));
            question.setSource(item.getSource());
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
            Integer pageSize) {
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
                resultPage.getPages());
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
                        .orderByAsc(InterviewQuestion::getSortOrder));

        return toDetailResponse(report, questions);
    }

    @Override
    @Transactional
    public Boolean delete(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport report = getUserQuestionReportOrThrow(reportId, currentUserId);

        interviewQuestionReportMapper.deleteById(report.getId());

        List<InterviewQuestion> questions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, report.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0));

        for (InterviewQuestion question : questions) {
            interviewQuestionMapper.deleteById(question.getId());
        }

        return true;
    }

    @Override
    @Transactional
    public InterviewQuestionGenerateResponse regenerate(Long reportId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        InterviewQuestionReport existingReport = getUserQuestionReportOrThrow(reportId, currentUserId);

        Resume resume = getUserResumeOrThrow(existingReport.getResumeId(), currentUserId);
        ResumeVersion resumeVersion = resolveResumeVersion(resume, existingReport.getResumeVersionId(), currentUserId);
        String resumeText = resumeVersion == null ? resume.getParsedText() : resumeVersion.getContent();
        JobDescription job = getUserJobOrThrow(existingReport.getJobId(), currentUserId);

        AnalysisReport analysisReport = null;
        if (existingReport.getAnalysisReportId() != null) {
            analysisReport = getUserAnalysisReportOrThrow(existingReport.getAnalysisReportId(), currentUserId);
        }

        List<InterviewQuestion> oldQuestions = interviewQuestionMapper.selectList(
                new LambdaQueryWrapper<InterviewQuestion>()
                        .eq(InterviewQuestion::getReportId, existingReport.getId())
                        .eq(InterviewQuestion::getUserId, currentUserId)
                        .eq(InterviewQuestion::getDeleted, 0));
        InterviewQuestionGenerateRequest regenerateRequest = buildRegenerateRequest(existingReport, oldQuestions);

        if (!StringUtils.hasText(resumeText)) {
            throw new BusinessException("简历解析文本为空，无法生成面试题");
        }

        if (!StringUtils.hasText(job.getJdContent())) {
            throw new BusinessException("岗位JD为空，无法生成面试题");
        }

        String analysisReportText = buildAnalysisReportText(analysisReport);
        String ragContext = buildRagContext(resumeText, job);
        if (StringUtils.hasText(ragContext)) {
            analysisReportText = analysisReportText + "\n\n【岗位知识库参考内容】\n" + ragContext;
        }

        String prompt = InterviewQuestionPromptBuilder.build(
                resumeText,
                job.getJdContent(),
                analysisReportText,
                regenerateRequest);

        String rawResponse = aiClient.chat(prompt);

        AiInterviewQuestionResult aiResult = parseInterviewQuestions(rawResponse, job, regenerateRequest);

        if (aiResult.getQuestions() == null || aiResult.getQuestions().isEmpty()) {
            throw new BusinessException("AI 未生成有效面试题");
        }

        String title = StringUtils.hasText(aiResult.getTitle())
                ? aiResult.getTitle()
                : job.getCompanyName() + " " + job.getJobTitle() + " 面试题准备";

        existingReport.setTitle(ensureChineseTitle(title, job));
        existingReport.setQuestionCount(aiResult.getQuestions().size());
        existingReport.setAiProvider(aiProperties.getProvider());
        existingReport.setAiModel(aiProperties.getModel());
        existingReport.setRawAiResponse(rawResponse);

        interviewQuestionReportMapper.updateById(existingReport);

        for (InterviewQuestion q : oldQuestions) {
            interviewQuestionMapper.deleteById(q.getId());
        }

        int sortOrder = 1;
        for (AiInterviewQuestionResult.QuestionItem item : aiResult.getQuestions()) {
            InterviewQuestion question = new InterviewQuestion();
            question.setReportId(existingReport.getId());
            question.setUserId(currentUserId);
            question.setQuestionType(normalizeQuestionType(item.getQuestionType()));
            question.setDifficulty(normalizeDifficulty(item.getDifficulty()));
            question.setQuestion(item.getQuestion());
            question.setAnswer(item.getAnswer());
            question.setAnswerPoints(JsonUtils.toJsonString(nullToEmpty(item.getAnswerPoints())));
            question.setRelatedSkills(JsonUtils.toJsonString(nullToEmpty(item.getRelatedSkills())));
            question.setFollowUps(JsonUtils.toJsonString(nullToEmpty(item.getFollowUps())));
            question.setKeywords(JsonUtils.toJsonString(nullToEmpty(item.getKeywords())));
            question.setSource(item.getSource());
            question.setSortOrder(sortOrder++);

            interviewQuestionMapper.insert(question);
        }

        return toGenerateResponse(existingReport, false);
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, 0)
                        .last("LIMIT 1"));

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
                        .last("LIMIT 1"));

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
                        .last("LIMIT 1"));

        if (report == null) {
            throw new BusinessException("分析报告不存在或无权限访问");
        }

        return report;
    }

    private String buildRagContext(String resumeText, JobDescription job) {
        try {
            RagSearchRequest request = new RagSearchRequest();
            request.setQuery(String.join("\n", List.of(
                    resumeText == null ? "" : resumeText,
                    job.getJobTitle() == null ? "" : job.getJobTitle(),
                    job.getJdContent() == null ? "" : job.getJdContent())));
            request.setDirection(StringUtils.hasText(job.getJobType()) ? job.getJobType() : null);
            request.setTopK(5);
            List<RagSearchResultResponse> results = ragKnowledgeService.search(request);
            if (results == null || results.isEmpty()) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            int index = 1;
            for (RagSearchResultResponse item : results) {
                builder.append(index++)
                        .append(". 【")
                        .append(item.getDirection())
                        .append(" / ")
                        .append(item.getKnowledgeType())
                        .append("】")
                        .append(item.getContent())
                        .append("\n");
            }
            return builder.toString();
        } catch (Exception e) {
            log.warn("构建 RAG 上下文失败，将使用普通 AI 生成面试题。resumeText length={}, jobId={}",
                    resumeText != null ? resumeText.length() : 0, job.getId(), e);
            return "";
        }
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
                        .last("LIMIT 1"));

        if (report == null) {
            throw new BusinessException("面试题报告不存在或无权限访问");
        }

        return report;
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
                report.getInterviewTips());
    }

    private AiInterviewQuestionResult parseInterviewQuestions(
            String rawResponse,
            JobDescription job,
            InterviewQuestionGenerateRequest request) {
        try {
            AiInterviewQuestionResult result = InterviewQuestionParser.parse(rawResponse);
            if (hasEnglishQuestion(result)) {
                log.warn("AI interview questions contain English content, using Chinese fallback. rawLength={}",
                        rawResponse == null ? 0 : rawResponse.length());
                return buildFallbackQuestions(job, request);
            }
            return normalizeQuestionResult(result, job, request);
        } catch (BusinessException e) {
            log.warn("AI interview question parse failed, using Chinese fallback. rawLength={}, reason={}",
                    rawResponse == null ? 0 : rawResponse.length(), e.getMessage());
            return buildFallbackQuestions(job, request);
        }
    }

    private boolean hasEnglishQuestion(AiInterviewQuestionResult result) {
        if (result == null || result.getQuestions() == null) {
            return false;
        }
        long englishLikeCount = result.getQuestions().stream()
                .filter(item -> isMostlyEnglish(item.getQuestion()) || isMostlyEnglish(item.getAnswer()))
                .count();
        return englishLikeCount > 0;
    }

    private boolean isMostlyEnglish(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        int asciiLetters = 0;
        int chineseChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                asciiLetters++;
            } else if (ch >= '\u4e00' && ch <= '\u9fff') {
                chineseChars++;
            }
        }
        return asciiLetters >= 20 && asciiLetters > chineseChars * 2;
    }

    private AiInterviewQuestionResult normalizeQuestionResult(
            AiInterviewQuestionResult result,
            JobDescription job,
            InterviewQuestionGenerateRequest request) {
        int targetCount = resolveQuestionCount(request);
        List<String> categories = resolveCategories(request);
        List<String> difficulties = resolveDifficulties(request);

        List<AiInterviewQuestionResult.QuestionItem> selected = new java.util.ArrayList<>();
        for (AiInterviewQuestionResult.QuestionItem item : nullToEmptyQuestions(result.getQuestions())) {
            item.setQuestionType(normalizeQuestionType(item.getQuestionType()));
            item.setDifficulty(normalizeDifficulty(item.getDifficulty()));
            if (categories.contains(item.getQuestionType()) && difficulties.contains(item.getDifficulty())) {
                selected.add(item);
            }
            if (selected.size() >= targetCount) {
                break;
            }
        }

        int index = 0;
        while (selected.size() < targetCount) {
            String category = categories.get(index % categories.size());
            String difficulty = difficulties.get(index % difficulties.size());
            selected.add(fallbackBySpec(category, difficulty, selected.size() + 1));
            index++;
        }

        applyAnswerOptions(selected, request);
        for (int i = 0; i < selected.size(); i++) {
            selected.get(i).setSortOrder(i + 1);
        }
        result.setTitle(ensureChineseTitle(result.getTitle(), job));
        result.setQuestions(selected);
        return result;
    }

    private AiInterviewQuestionResult buildFallbackQuestions(
            JobDescription job,
            InterviewQuestionGenerateRequest request) {
        String company = StringUtils.hasText(job.getCompanyName()) ? job.getCompanyName() : "目标公司";
        String title = StringUtils.hasText(job.getJobTitle()) ? job.getJobTitle() : "目标岗位";

        AiInterviewQuestionResult result = new AiInterviewQuestionResult();
        result.setTitle(company + " " + title + " 面试题准备");
        return normalizeQuestionResult(result, job, request);
    }

    private AiInterviewQuestionResult.QuestionItem fallbackBySpec(String category, String difficulty, int index) {
        return switch (category) {
            case "JAVA_BASIC" -> fallbackItem(category, difficulty,
                    "请说明 Java 基础中第 " + index + " 个常见考点，并结合项目举例。",
                    "回答时先解释核心概念，再说明典型使用场景，最后结合自己的项目说明为什么这样设计。",
                    List.of("Java 基础", "核心概念", "项目应用"));
            case "SPRING_BOOT" -> fallbackItem(category, difficulty,
                    "Spring Boot 在项目中如何完成配置加载和 Bean 装配？",
                    "可以从自动配置、条件注解、配置属性绑定和 Starter 机制说明，并结合项目中的实际模块展开。",
                    List.of("Spring Boot", "自动配置", "Bean 装配"));
            case "SPRING_SECURITY" -> fallbackItem(category, difficulty,
                    "Spring Security 与 JWT 结合时，认证和授权流程如何串起来？",
                    "登录后签发 JWT，后续请求通过过滤器解析 Token，构造 Authentication，再由权限注解完成接口授权。",
                    List.of("Spring Security", "JWT", "权限校验"));
            case "MYSQL" -> fallbackItem(category, difficulty,
                    "MySQL 查询变慢时，你会从哪些角度排查？",
                    "可以从索引命中、执行计划、慢 SQL、分页方式、表结构和事务锁等待等角度排查。",
                    List.of("MySQL", "索引", "执行计划"));
            case "REDIS" -> fallbackItem(category, difficulty,
                    "Redis 在 AI 分析任务中适合保存哪些数据？需要注意什么？",
                    "适合保存短期任务状态、缓存结果和进度信息，需要注意过期时间、key 设计和缓存一致性。",
                    List.of("Redis", "缓存", "任务状态"));
            case "PROJECT" -> fallbackItem(category, difficulty,
                    "请介绍 InternPilot 项目中一个你认为最能体现工程能力的模块。",
                    "可以说明业务目标、技术选型、核心流程、遇到的问题、解决方案和最终效果。",
                    List.of("项目经历", "工程能力", "问题解决"));
            case "HR" -> fallbackItem(category, difficulty,
                    "如果实习中接到一个不熟悉的任务，你会如何推进？",
                    "可以从明确需求、拆分任务、查阅资料、请教同事、先做最小可运行版本和及时反馈几个方面回答。",
                    List.of("沟通", "学习能力", "执行力"));
            case "RESUME" -> fallbackItem(category, difficulty,
                    "请结合你的简历，说明一个项目从需求到上线的完整过程。",
                    "回答时按背景、职责、技术实现、难点、结果和复盘展开，突出自己真实参与的部分。",
                    List.of("简历深挖", "项目复盘", "个人职责"));
            default -> fallbackItem("JOB_SKILL", difficulty,
                    "针对目标岗位的核心技能，你认为自己最需要补强哪一点？",
                    "先对照岗位 JD 识别关键能力，再结合自己的项目经验说明优势和短板，并给出补强计划。",
                    List.of("岗位技能", "JD 分析", "补强计划"));
        };
    }

    private AiInterviewQuestionResult.QuestionItem fallbackItem(
            String type,
            String difficulty,
            String question,
            String answer,
            List<String> answerPoints) {
        AiInterviewQuestionResult.QuestionItem item = new AiInterviewQuestionResult.QuestionItem();
        item.setQuestionType(type);
        item.setDifficulty(difficulty);
        item.setQuestion(question);
        item.setAnswer(answer);
        item.setAnswerPoints(answerPoints);
        item.setRelatedSkills(answerPoints);
        item.setFollowUps(List.of("请结合你的项目经历举一个例子。", "如果线上出现相关问题，你会如何排查？"));
        item.setKeywords(answerPoints);
        item.setSource("AI 输出不稳定时由系统生成的中文兜底题目");
        return item;
    }

    private List<AiInterviewQuestionResult.QuestionItem> nullToEmptyQuestions(
            List<AiInterviewQuestionResult.QuestionItem> questions) {
        return questions == null ? Collections.emptyList() : questions;
    }

    private void applyAnswerOptions(
            List<AiInterviewQuestionResult.QuestionItem> questions,
            InterviewQuestionGenerateRequest request) {
        if (request == null) {
            return;
        }
        if (Boolean.FALSE.equals(request.getIncludeAnswer())) {
            for (AiInterviewQuestionResult.QuestionItem item : questions) {
                item.setAnswer("");
                item.setAnswerPoints(Collections.emptyList());
            }
        }
        if (Boolean.FALSE.equals(request.getIncludeFollowUps())) {
            for (AiInterviewQuestionResult.QuestionItem item : questions) {
                item.setFollowUps(Collections.emptyList());
            }
        }
    }

    private InterviewQuestionGenerateRequest buildRegenerateRequest(
            InterviewQuestionReport report,
            List<InterviewQuestion> oldQuestions) {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setResumeId(report.getResumeId());
        request.setResumeVersionId(report.getResumeVersionId());
        request.setJobId(report.getJobId());
        request.setAnalysisReportId(report.getAnalysisReportId());
        int questionCount = report.getQuestionCount() == null ? 0 : report.getQuestionCount();
        if (questionCount <= 0 && oldQuestions != null) {
            questionCount = oldQuestions.size();
        }
        request.setQuestionCount(questionCount <= 0 ? DEFAULT_QUESTION_COUNT : questionCount);
        if (oldQuestions != null && !oldQuestions.isEmpty()) {
            request.setCategories(oldQuestions.stream()
                    .map(InterviewQuestion::getQuestionType)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList());
            request.setDifficulties(oldQuestions.stream()
                    .map(InterviewQuestion::getDifficulty)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList());
        }
        request.setIncludeAnswer(true);
        request.setIncludeFollowUps(true);
        return request;
    }

    private int resolveQuestionCount(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getQuestionCount() == null) {
            return DEFAULT_QUESTION_COUNT;
        }
        return Math.max(MIN_QUESTION_COUNT, Math.min(MAX_QUESTION_COUNT, request.getQuestionCount()));
    }

    private List<String> resolveCategories(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getCategories() == null || request.getCategories().isEmpty()) {
            return DEFAULT_CATEGORIES;
        }
        List<String> values = request.getCategories().stream()
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .filter(InterviewQuestionTypeEnum::isValid)
                .distinct()
                .toList();
        return values.isEmpty() ? DEFAULT_CATEGORIES : values;
    }

    private List<String> resolveDifficulties(InterviewQuestionGenerateRequest request) {
        if (request == null || request.getDifficulties() == null || request.getDifficulties().isEmpty()) {
            return DEFAULT_DIFFICULTIES;
        }
        List<String> values = request.getDifficulties().stream()
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .filter(QuestionDifficultyEnum::isValid)
                .distinct()
                .toList();
        return values.isEmpty() ? DEFAULT_DIFFICULTIES : values;
    }

    private String ensureChineseTitle(String title, JobDescription job) {
        if (StringUtils.hasText(title) && !isMostlyEnglish(title)) {
            return title;
        }
        return (StringUtils.hasText(job.getCompanyName()) ? job.getCompanyName() : "目标公司")
                + " "
                + (StringUtils.hasText(job.getJobTitle()) ? job.getJobTitle() : "目标岗位")
                + " 面试题准备";
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
            List<InterviewQuestion> questions) {
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
        response.setAnswerPoints(JsonUtils.toStringList(question.getAnswerPoints()));
        response.setRelatedSkills(JsonUtils.toStringList(question.getRelatedSkills()));
        response.setFollowUps(JsonUtils.toStringList(question.getFollowUps()));
        response.setKeywords(JsonUtils.toStringList(question.getKeywords()));
        response.setSource(question.getSource());
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
                        .last("LIMIT 1"));
    }

    private ResumeVersion getUserResumeVersionOrThrow(Long resumeId, Long versionId, Long userId) {
        ResumeVersion version = resumeVersionMapper.selectOne(
                new LambdaQueryWrapper<ResumeVersion>()
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
}
