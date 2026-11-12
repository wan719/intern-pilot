package com.internpilot.service.resume.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.ai.client.AiChatRequest;
import com.internpilot.ai.client.AiClient;
import com.internpilot.ai.prompt.AiOutputFormat;
import com.internpilot.ai.prompt.AiPromptContext;
import com.internpilot.ai.prompt.template.AiPromptTemplate;
import com.internpilot.ai.prompt.template.AiPromptTemplateResolver;
import com.internpilot.ai.router.AiModelRouter;
import com.internpilot.ai.scenario.AiScenarioEnum;
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
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.resume.ResumeVersionCompareResponse;
import com.internpilot.vo.resume.ResumeVersionCreateResponse;
import com.internpilot.vo.resume.ResumeVersionDetailResponse;
import com.internpilot.vo.resume.ResumeVersionListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeVersionServiceImplTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private ResumeVersionMapper resumeVersionMapper;

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @Mock
    private AnalysisReportMapper analysisReportMapper;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiModelRouter aiModelRouter;

    @Mock
    private AiPromptTemplateResolver promptTemplateResolver;

    @InjectMocks
    private ResumeVersionServiceImpl service;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldPersistManualVersionAndDefaultCurrentFlag() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        when(resumeVersionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            ResumeVersion version = invocation.getArgument(0);
            version.setId(10L);
            version.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(resumeVersionMapper).insert(any(ResumeVersion.class));

        ResumeVersionCreateRequest request = new ResumeVersionCreateRequest();
        request.setVersionName("Java后端优化版");
        request.setContent("Java Spring Boot Redis\n项目经历");

        ResumeVersionCreateResponse response = service.create(1L, request);

        assertEquals(10L, response.getVersionId());
        assertEquals(ResumeVersionTypeEnum.MANUAL.getCode(), response.getVersionType());
        assertEquals(1, response.getIsCurrent());

        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("Java后端优化版", captor.getValue().getVersionName());
        assertTrue(captor.getValue().getContentSummary().contains("Spring Boot"));
    }

    @Test
    void createShouldValidateTargetJobAndSourceVersion() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job());
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version(20L, "原始版本", "line"));
        when(resumeVersionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        doAnswer(invocation -> {
            ResumeVersion version = invocation.getArgument(0);
            version.setId(11L);
            return 1;
        }).when(resumeVersionMapper).insert(any(ResumeVersion.class));

        ResumeVersionCreateRequest request = new ResumeVersionCreateRequest();
        request.setVersionName("岗位定制版");
        request.setVersionType(ResumeVersionTypeEnum.JOB_TARGETED.getCode());
        request.setContent("针对岗位优化");
        request.setTargetJobId(100L);
        request.setSourceVersionId(20L);

        ResumeVersionCreateResponse response = service.create(1L, request);

        assertEquals(11L, response.getVersionId());
        assertEquals(0, response.getIsCurrent());
        verify(jobDescriptionMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    void createShouldRejectInvalidVersionType() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());

        ResumeVersionCreateRequest request = new ResumeVersionCreateRequest();
        request.setVersionName("错误类型");
        request.setVersionType("WRONG");
        request.setContent("content");

        assertThrows(BusinessException.class, () -> service.create(1L, request));
    }

    @Test
    void listAndDetailShouldMapJobInfo() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        ResumeVersion version = version(20L, "岗位版", "content");
        version.setTargetJobId(100L);
        when(resumeVersionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(version));
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version);
        when(jobDescriptionMapper.selectById(100L)).thenReturn(job());

        List<ResumeVersionListResponse> list = service.list(1L);
        ResumeVersionDetailResponse detail = service.getDetail(1L, 20L);

        assertEquals(1, list.size());
        assertEquals("InternPilot", list.get(0).getTargetCompanyName());
        assertEquals("Java实习生", detail.getTargetJobTitle());
    }

    @Test
    void updateAndSetCurrentShouldPersistChanges() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(version(20L, "旧版本", "旧内容"));

        ResumeVersionUpdateRequest updateRequest = new ResumeVersionUpdateRequest();
        updateRequest.setVersionName("新版本");
        updateRequest.setContent("新的简历内容");

        assertTrue(service.update(1L, 20L, updateRequest));
        assertTrue(service.setCurrent(1L, 20L));

        verify(resumeVersionMapper).update(any(ResumeVersion.class), any(LambdaQueryWrapper.class));
        verify(resumeVersionMapper, org.mockito.Mockito.atLeastOnce()).updateById(any(ResumeVersion.class));
    }

    @Test
    void deleteShouldRejectCurrentOriginalAndLastVersion() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());

        ResumeVersion current = version(20L, "当前版本", "content");
        current.setIsCurrent(1);
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(current);
        assertThrows(BusinessException.class, () -> service.delete(1L, 20L));

        ResumeVersion original = version(21L, "原始版本", "content");
        original.setVersionType(ResumeVersionTypeEnum.ORIGINAL.getCode());
        original.setIsCurrent(0);
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(original);
        assertThrows(BusinessException.class, () -> service.delete(1L, 21L));

        ResumeVersion manual = version(22L, "手动版本", "content");
        manual.setIsCurrent(0);
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(manual);
        when(resumeVersionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.delete(1L, 22L));
    }

    @Test
    void deleteShouldRemoveNonCurrentManualVersion() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        ResumeVersion manual = version(22L, "手动版本", "content");
        manual.setIsCurrent(0);
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(manual);
        when(resumeVersionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        assertTrue(service.delete(1L, 22L));

        verify(resumeVersionMapper).deleteById(22L);
    }

    @Test
    void compareShouldReturnAddedRemovedAndCommonLines() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        ResumeVersion oldVersion = version(1L, "旧", "A\nB\nC");
        ResumeVersion newVersion = version(2L, "新", "B\nC\nD");
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(oldVersion, newVersion);

        ResumeVersionCompareResponse response = service.compare(1L, 1L, 2L);

        assertEquals(List.of("D"), response.getAddedLines());
        assertEquals(List.of("A"), response.getRemovedLines());
        assertEquals(List.of("B", "C"), response.getCommonLines());
        assertEquals(1, response.getAddedCount());
        assertEquals(1, response.getRemovedCount());
    }

    @Test
    void optimizeShouldCreateAiOptimizedVersion() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(version(20L, "原始版本", "原始简历"));
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job());
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report());

        AiPromptTemplate template = org.mockito.Mockito.mock(AiPromptTemplate.class);
        when(promptTemplateResolver.resolve(AiScenarioEnum.RESUME_OPTIMIZATION)).thenReturn(template);
        when(template.version()).thenReturn("resume-opt-v1");
        when(template.systemPrompt()).thenReturn("system");
        when(template.outputFormat()).thenReturn(AiOutputFormat.PLAIN_TEXT);
        when(template.buildUserPrompt(any(AiPromptContext.class))).thenReturn("prompt");
        when(aiModelRouter.route(AiScenarioEnum.RESUME_OPTIMIZATION)).thenReturn("deepseek-chat");
        when(aiModelRouter.fallback(AiScenarioEnum.RESUME_OPTIMIZATION)).thenReturn("deepseek-reasoner");
        when(aiModelRouter.allowFallback(AiScenarioEnum.RESUME_OPTIMIZATION)).thenReturn(true);
        when(aiClient.chat(any(AiChatRequest.class))).thenReturn("优化后的简历内容");
        doAnswer(invocation -> {
            ResumeVersion version = invocation.getArgument(0);
            version.setId(99L);
            return 1;
        }).when(resumeVersionMapper).insert(any(ResumeVersion.class));

        ResumeVersionOptimizeRequest request = new ResumeVersionOptimizeRequest();
        request.setSourceVersionId(20L);
        request.setTargetJobId(100L);
        request.setAiReportId(30L);
        request.setExtraRequirement("突出项目");

        ResumeVersionCreateResponse response = service.optimize(1L, request);

        assertEquals(99L, response.getVersionId());
        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        assertEquals(ResumeVersionTypeEnum.AI_OPTIMIZED.getCode(), captor.getValue().getVersionType());
        assertEquals("优化后的简历内容", captor.getValue().getContent());
        assertNotNull(captor.getValue().getOptimizePrompt());
    }

    @Test
    void optimizeShouldRejectEmptyAiResponseAndMismatchedReport() {
        mockLoginUser(7L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume());
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(version(20L, "原始版本", "原始简历"));
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job());

        AnalysisReport mismatched = report();
        mismatched.setResumeId(2L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mismatched);

        ResumeVersionOptimizeRequest request = new ResumeVersionOptimizeRequest();
        request.setSourceVersionId(20L);
        request.setTargetJobId(100L);
        request.setAiReportId(30L);

        assertThrows(BusinessException.class, () -> service.optimize(1L, request));

        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.optimize(1L, request));
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails principal = new CustomUserDetails(userId, "tester", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Resume resume() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(7L);
        resume.setDeleted(0);
        return resume;
    }

    private ResumeVersion version(Long id, String name, String content) {
        ResumeVersion version = new ResumeVersion();
        version.setId(id);
        version.setUserId(7L);
        version.setResumeId(1L);
        version.setVersionName(name);
        version.setVersionType(ResumeVersionTypeEnum.MANUAL.getCode());
        version.setContent(content);
        version.setContentSummary(content);
        version.setIsCurrent(0);
        version.setDeleted(0);
        version.setCreatedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        return version;
    }

    private JobDescription job() {
        JobDescription job = new JobDescription();
        job.setId(100L);
        job.setUserId(7L);
        job.setCompanyName("InternPilot");
        job.setJobTitle("Java实习生");
        job.setJdContent("需要 Java Spring Boot Redis");
        job.setDeleted(0);
        return job;
    }

    private AnalysisReport report() {
        AnalysisReport report = new AnalysisReport();
        report.setId(30L);
        report.setUserId(7L);
        report.setResumeId(1L);
        report.setJobId(100L);
        report.setMatchScore(88);
        report.setMatchLevel("HIGH");
        report.setStrengths("Java");
        report.setWeaknesses("项目深度");
        report.setMissingSkills("Redis");
        report.setSuggestions("补充缓存项目");
        report.setInterviewTips("准备八股");
        report.setDeleted(0);
        return report;
    }
}
