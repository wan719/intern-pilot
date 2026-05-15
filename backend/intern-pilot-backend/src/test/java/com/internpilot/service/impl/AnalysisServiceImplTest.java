package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.config.AiProperties;
import com.internpilot.dto.analysis.AnalysisMatchRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.Resume;
import com.internpilot.exception.AiServiceException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.AiClient;
import com.internpilot.service.RagKnowledgeService;
import com.internpilot.vo.analysis.AnalysisReportDetailResponse;
import com.internpilot.vo.analysis.AnalysisReportListResponse;
import com.internpilot.vo.analysis.AnalysisResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

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
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RagKnowledgeService ragKnowledgeService;

    private final AiProperties aiProperties = new AiProperties();

    private AnalysisServiceImpl analysisService;

    AnalysisServiceImplTest() {
        aiProperties.setProvider("deepseek");
        aiProperties.setModel("deepseek-chat");
    }

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisServiceImpl(
                resumeMapper,
                resumeVersionMapper,
                jobDescriptionMapper,
                analysisReportMapper,
                aiClient,
                aiProperties,
                redisTemplate,
                new ObjectMapper(),
                ragKnowledgeService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchShouldReturnCachedResultWhenAvailable() {
        mockLoginUser(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AnalysisResultResponse cached = new AnalysisResultResponse();
        cached.setReportId(100L);
        cached.setResumeId(1L);
        cached.setJobId(2L);
        cached.setMatchScore(88);
        cached.setMatchLevel("HIGH");
        cached.setStrengths(List.of("强项"));
        cached.setWeaknesses(List.of("短板"));
        cached.setMissingSkills(List.of("Docker"));
        cached.setSuggestions(List.of("补充部署"));
        cached.setInterviewTips(List.of("准备面试"));
        cached.setCacheHit(false);
        cached.setCreatedAt(LocalDateTime.now());

        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildJob());
        when(valueOperations.get(anyString())).thenReturn(cached);

        AnalysisMatchRequest request = new AnalysisMatchRequest();
        request.setResumeId(1L);
        request.setJobId(2L);
        request.setForceRefresh(false);

        AnalysisResultResponse response = analysisService.match(request);

        assertTrue(response.getCacheHit());
        assertEquals(88, response.getMatchScore());
        verify(aiClient, never()).chat(anyString());
    }

    @Test
    void matchShouldCallAiAndPersistReport() {
        mockLoginUser(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildJob());
        when(aiClient.chat(anyString())).thenReturn("""
                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": ["Spring Boot 项目经验"],
                  "weaknesses": ["缺少实习经历"],
                  "missingSkills": ["Docker"],
                  "suggestions": ["补充部署经验"],
                  "interviewTips": ["准备 Redis 场景题"]
                }
                """);
        doAnswer(invocation -> {
            AnalysisReport report = invocation.getArgument(0);
            report.setId(99L);
            report.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(analysisReportMapper).insert(any(AnalysisReport.class));

        AnalysisMatchRequest request = new AnalysisMatchRequest();
        request.setResumeId(1L);
        request.setJobId(2L);
        request.setForceRefresh(false);

        AnalysisResultResponse response = analysisService.match(request);

        assertEquals(99L, response.getReportId());
        assertEquals(82, response.getMatchScore());
        assertFalse(response.getCacheHit());
        verify(valueOperations).set(anyString(), any(AnalysisResultResponse.class), any());
    }

    @Test
    void listReportsShouldMapPageResult() {
        mockLoginUser(1L);

        AnalysisReport report = new AnalysisReport();
        report.setId(1L);
        report.setResumeId(10L);
        report.setJobId(20L);
        report.setMatchScore(75);
        report.setMatchLevel("MEDIUM_HIGH");
        report.setCacheHit(0);
        report.setCreatedAt(LocalDateTime.now());

        Page<AnalysisReport> page = new Page<>(1, 10);
        page.setRecords(List.of(report));
        page.setTotal(1L);
        page.setPages(1L);

        when(analysisReportMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);
        when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJob());

        var result = analysisService.listReports(null, null, 70, 1, 10);

        assertEquals(1L, result.getTotal());
        AnalysisReportListResponse first = result.getRecords().get(0);
        assertEquals("腾讯", first.getCompanyName());
        assertEquals("Java后端开发实习生", first.getJobTitle());
    }

    @Test
    void getReportDetailShouldReturnJoinedFields() {
        mockLoginUser(1L);

        AnalysisReport report = new AnalysisReport();
        report.setId(5L);
        report.setUserId(1L);
        report.setResumeId(1L);
        report.setJobId(2L);
        report.setMatchScore(80);
        report.setMatchLevel("MEDIUM_HIGH");
        report.setStrengths("[\"Spring Boot\"]");
        report.setWeaknesses("[\"缺少实习\"]");
        report.setMissingSkills("[\"Docker\"]");
        report.setSuggestions("[\"补充部署\"]");
        report.setInterviewTips("[\"准备缓存题\"]");
        report.setAiProvider("deepseek");
        report.setAiModel("deepseek-chat");
        report.setCacheHit(0);
        report.setCreatedAt(LocalDateTime.now());

        Resume resume = buildResume();
        resume.setResumeName("Java后端简历");

        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(resumeMapper.selectById(1L)).thenReturn(resume);
        when(jobDescriptionMapper.selectById(2L)).thenReturn(buildJob());

        AnalysisReportDetailResponse response = analysisService.getReportDetail(5L);

        assertEquals("Java后端简历", response.getResumeName());
        assertEquals("腾讯", response.getCompanyName());
        assertEquals(1, response.getStrengths().size());
    }

    @Test
    void matchShouldContinueWhenRagFails() {
        mockLoginUser(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildJob());
        when(ragKnowledgeService.search(any())).thenThrow(new RuntimeException("RAG 服务异常"));
        when(aiClient.chat(anyString())).thenReturn("""
                {
                  "matchScore": 75,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": ["Spring Boot"],
                  "weaknesses": ["缺少实习"],
                  "missingSkills": ["Docker"],
                  "suggestions": ["补充部署"],
                  "interviewTips": ["准备面试"]
                }
                """);
        doAnswer(invocation -> {
            AnalysisReport report = invocation.getArgument(0);
            report.setId(100L);
            report.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(analysisReportMapper).insert(any(AnalysisReport.class));

        AnalysisMatchRequest request = new AnalysisMatchRequest();
        request.setResumeId(1L);
        request.setJobId(2L);
        request.setForceRefresh(false);

        AnalysisResultResponse response = analysisService.match(request);

        assertNotNull(response);
        assertEquals(75, response.getMatchScore());
        assertFalse(response.getCacheHit());
        verify(aiClient).chat(anyString());
    }

    @Test
    void matchShouldThrowWhenAiReturnsEmpty() {
        mockLoginUser(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildJob());
        when(aiClient.chat(anyString())).thenReturn("");

        AnalysisMatchRequest request = new AnalysisMatchRequest();
        request.setResumeId(1L);
        request.setJobId(2L);
        request.setForceRefresh(false);

        assertThrows(AiServiceException.class, () -> analysisService.match(request));
    }

    @Test
    void matchShouldUseFallbackWhenAiReturnsInvalidJson() {
        mockLoginUser(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume());
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildJob());
        when(aiClient.chat(anyString())).thenReturn("这不是合法的 JSON 格式");
        doAnswer(invocation -> {
            AnalysisReport report = invocation.getArgument(0);
            report.setId(100L);
            report.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(analysisReportMapper).insert(any(AnalysisReport.class));

        AnalysisMatchRequest request = new AnalysisMatchRequest();
        request.setResumeId(1L);
        request.setJobId(2L);
        request.setForceRefresh(false);

        AnalysisResultResponse response = analysisService.match(request);

        assertEquals(100L, response.getReportId());
        assertEquals(60, response.getMatchScore());
        assertEquals("MEDIUM", response.getMatchLevel());
        assertTrue(response.getWeaknesses().get(0).contains("兜底解析"));
    }

    @Test
    void deleteReportShouldDeleteOwnReport() {
        mockLoginUser(1L);

        AnalysisReport report = new AnalysisReport();
        report.setId(10L);
        report.setUserId(1L);
        report.setDeleted(0);

        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(analysisReportMapper.deleteById(10L)).thenReturn(1);

        analysisService.deleteReport(10L);

        verify(analysisReportMapper).deleteById(10L);
    }

    @Test
    void deleteReportShouldThrowWhenReportNotFound() {
        mockLoginUser(1L);

        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(com.internpilot.exception.BusinessException.class,
                () -> analysisService.deleteReport(999L));
    }

    @Test
    void deleteReportShouldThrowWhenNotOwner() {
        mockLoginUser(1L);

        AnalysisReport report = new AnalysisReport();
        report.setId(10L);
        report.setUserId(2L);

        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);

        assertThrows(com.internpilot.exception.BusinessException.class,
                () -> analysisService.deleteReport(10L));
    }

    private Resume buildResume() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1L);
        resume.setParsedText("熟悉 Java、Spring Boot、MySQL、Redis");
        return resume;
    }

    private JobDescription buildJob() {
        JobDescription job = new JobDescription();
        job.setId(2L);
        job.setUserId(1L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        job.setJdContent("要求熟悉 Java、Spring Boot、MySQL、Redis、Docker");
        return job;
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "wan", "USER");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
