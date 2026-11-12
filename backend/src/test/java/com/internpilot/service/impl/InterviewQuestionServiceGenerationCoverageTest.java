package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.ai.client.AiChatRequest;
import com.internpilot.ai.client.AiClient;
import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.prompt.template.AiPromptTemplate;
import com.internpilot.ai.prompt.template.AiPromptTemplateResolver;
import com.internpilot.ai.router.AiModelRouter;
import com.internpilot.ai.scenario.AiScenarioEnum;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.interview.InterviewQuestionGenerateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.InterviewQuestion;
import com.internpilot.entity.InterviewQuestionReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.InterviewQuestionMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.interview.impl.InterviewQuestionServiceImpl;
import com.internpilot.service.rag.RagKnowledgeService;
import com.internpilot.vo.interview.InterviewQuestionGenerateResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionServiceGenerationCoverageTest {

    private static final List<String> ALL_CATEGORIES = List.of(
            "JAVA_BASIC", "SPRING_BOOT", "SPRING_SECURITY", "MYSQL", "REDIS",
            "PROJECT", "HR", "RESUME", "JOB_SKILL"
    );

    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeVersionMapper resumeVersionMapper;
    @Mock
    private JobDescriptionMapper jobDescriptionMapper;
    @Mock
    private AnalysisReportMapper analysisReportMapper;
    @Mock
    private InterviewQuestionReportMapper reportMapper;
    @Mock
    private InterviewQuestionMapper questionMapper;
    @Mock
    private AiClient aiClient;
    @Mock
    private RagKnowledgeService ragKnowledgeService;
    @Mock
    private AiModelRouter aiModelRouter;
    @Mock
    private AiPromptTemplateResolver promptTemplateResolver;
    @Mock
    private AiPromptTemplate promptTemplate;

    private InterviewQuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        AiProperties aiProperties = new AiProperties();
        aiProperties.setProvider("deepseek");
        service = new InterviewQuestionServiceImpl(
                resumeMapper,
                resumeVersionMapper,
                jobDescriptionMapper,
                analysisReportMapper,
                reportMapper,
                questionMapper,
                aiClient,
                aiProperties,
                ragKnowledgeService,
                aiModelRouter,
                promptTemplateResolver
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateShouldFallbackToChineseQuestionSetWhenAiJsonInvalid() {
        mockLoginUser(7L);
        mockResumeVersionAndJob();
        mockPrompt(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION);
        when(aiClient.chat(any(AiChatRequest.class))).thenReturn("not-json");
        when(ragKnowledgeService.search(any())).thenReturn(List.of(buildRagResult()));
        mockReportInsert(900L);

        InterviewQuestionGenerateRequest request = buildGenerateRequest();
        request.setIncludeAnswer(false);
        request.setIncludeFollowUps(false);

        InterviewQuestionGenerateResponse response = service.generate(request);

        assertThat(response.getReportId()).isEqualTo(900L);
        assertThat(response.getQuestionCount()).isEqualTo(9);

        ArgumentCaptor<AiChatRequest> aiRequestCaptor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiClient).chat(aiRequestCaptor.capture());
        assertThat(aiRequestCaptor.getValue().getScenario()).isEqualTo(AiScenarioEnum.INTERVIEW_QUESTION_GENERATION);
        assertThat(aiRequestCaptor.getValue().getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(aiRequestCaptor.getValue().getPromptVersion()).isEqualTo("interview-v-test");

        ArgumentCaptor<InterviewQuestion> questionCaptor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(questionMapper, org.mockito.Mockito.times(9)).insert(questionCaptor.capture());
        assertThat(questionCaptor.getAllValues())
                .extracting(InterviewQuestion::getQuestionType)
                .containsExactlyElementsOf(ALL_CATEGORIES);
        assertThat(questionCaptor.getAllValues())
                .allSatisfy(question -> {
                    assertThat(question.getAnswer()).isEmpty();
                    assertThat(question.getFollowUps()).isEqualTo("[]");
                    assertThat(question.getSortOrder()).isBetween(1, 9);
                });
    }

    @Test
    void regenerateShouldReplaceDuplicateOldQuestionsWithFreshFallbacks() {
        mockLoginUser(7L);
        mockResumeVersionAndJob();
        mockPrompt(AiScenarioEnum.INTERVIEW_QUESTION_REGENERATION);
        when(ragKnowledgeService.search(any())).thenReturn(null);

        InterviewQuestionReport existingReport = buildReport(500L);
        existingReport.setQuestionCount(9);
        when(reportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingReport);
        List<InterviewQuestion> oldQuestions = oldQuestions();
        when(questionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(oldQuestions);
        when(aiClient.chat(any(AiChatRequest.class))).thenReturn(duplicateQuestionJson(oldQuestions));

        InterviewQuestionGenerateResponse response = service.regenerate(500L);

        assertThat(response.getReportId()).isEqualTo(500L);
        verify(reportMapper).updateById(existingReport);
        for (InterviewQuestion question : oldQuestions) {
            verify(questionMapper).deleteById(question.getId());
        }

        ArgumentCaptor<InterviewQuestion> questionCaptor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(questionMapper, org.mockito.Mockito.times(9)).insert(questionCaptor.capture());
        assertThat(questionCaptor.getAllValues())
                .extracting(InterviewQuestion::getQuestionType)
                .containsExactlyElementsOf(ALL_CATEGORIES);
        assertThat(questionCaptor.getAllValues())
                .extracting(InterviewQuestion::getQuestion)
                .noneMatch(value -> value != null && value.startsWith("Q"));
    }

    @Test
    void generateShouldRejectMismatchedAnalysisReportScope() {
        mockLoginUser(7L);
        mockResumeVersionAndJob();
        AnalysisReport report = new AnalysisReport();
        report.setId(300L);
        report.setUserId(7L);
        report.setResumeId(999L);
        report.setJobId(20L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);

        InterviewQuestionGenerateRequest request = buildGenerateRequest();
        request.setAnalysisReportId(300L);

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(BusinessException.class);
    }

    private void mockResumeVersionAndJob() {
        Resume resume = new Resume();
        resume.setId(10L);
        resume.setUserId(7L);
        resume.setResumeName("resume");
        resume.setParsedText("Java Spring Boot MySQL Redis project backend");
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);

        ResumeVersion version = new ResumeVersion();
        version.setId(11L);
        version.setResumeId(10L);
        version.setUserId(7L);
        version.setContent("Java Spring Boot MySQL Redis project backend version");
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);

        JobDescription job = buildJob();
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);
    }

    private void mockPrompt(AiScenarioEnum scenario) {
        when(promptTemplateResolver.resolve(scenario)).thenReturn(promptTemplate);
        when(promptTemplate.version()).thenReturn("interview-v-test");
        when(promptTemplate.systemPrompt()).thenReturn("system");
        when(promptTemplate.outputFormat()).thenReturn(AiOutputFormat.JSON_OBJECT);
        when(promptTemplate.buildUserPrompt(any(AiPromptContext.class))).thenReturn("prompt");
        when(aiModelRouter.route(scenario)).thenReturn("deepseek-v4-flash");
        when(aiModelRouter.fallback(scenario)).thenReturn("deepseek-v4-pro");
        when(aiModelRouter.allowFallback(scenario)).thenReturn(true);
    }

    private void mockReportInsert(Long id) {
        doAnswer(invocation -> {
            InterviewQuestionReport report = invocation.getArgument(0);
            report.setId(id);
            report.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(reportMapper).insert(any(InterviewQuestionReport.class));
    }

    private InterviewQuestionGenerateRequest buildGenerateRequest() {
        InterviewQuestionGenerateRequest request = new InterviewQuestionGenerateRequest();
        request.setResumeId(10L);
        request.setJobId(20L);
        request.setQuestionCount(9);
        request.setCategories(ALL_CATEGORIES);
        request.setDifficulties(List.of("MEDIUM"));
        request.setIncludeAnswer(true);
        request.setIncludeFollowUps(true);
        return request;
    }

    private JobDescription buildJob() {
        JobDescription job = new JobDescription();
        job.setId(20L);
        job.setUserId(7L);
        job.setCompanyName("ACME");
        job.setJobTitle("Backend Intern");
        job.setJobType("backend");
        job.setJdContent("Java Spring Boot MySQL Redis API");
        return job;
    }

    private InterviewQuestionReport buildReport(Long id) {
        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setId(id);
        report.setUserId(7L);
        report.setResumeId(10L);
        report.setResumeVersionId(11L);
        report.setJobId(20L);
        report.setTitle("old report");
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }

    private List<InterviewQuestion> oldQuestions() {
        List<InterviewQuestion> questions = new ArrayList<>();
        for (int i = 0; i < ALL_CATEGORIES.size(); i++) {
            InterviewQuestion question = new InterviewQuestion();
            question.setId((long) i + 1);
            question.setQuestionType(ALL_CATEGORIES.get(i));
            question.setDifficulty("MEDIUM");
            question.setQuestion("Q" + (i + 1));
            question.setAnswer("A" + (i + 1));
            questions.add(question);
        }
        return questions;
    }

    private String duplicateQuestionJson(List<InterviewQuestion> oldQuestions) {
        StringBuilder builder = new StringBuilder("{\"title\":\"t\",\"questions\":[");
        for (int i = 0; i < oldQuestions.size(); i++) {
            InterviewQuestion question = oldQuestions.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append("{\"questionType\":\"")
                    .append(question.getQuestionType())
                    .append("\",\"difficulty\":\"MEDIUM\",\"question\":\"")
                    .append(question.getQuestion())
                    .append("\",\"answer\":\"A\",\"answerPoints\":[\"p\"]}");
        }
        builder.append("]}");
        return builder.toString();
    }

    private RagSearchResultResponse buildRagResult() {
        RagSearchResultResponse response = new RagSearchResultResponse();
        response.setDirection("backend");
        response.setKnowledgeType("SKILL_REQUIREMENT");
        response.setContent("Java Redis project knowledge");
        response.setSimilarity(0.9);
        return response;
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}
