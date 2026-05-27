package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.internpilot.dto.recommendation.JobRecommendationGenerateRequest;
import com.internpilot.entity.AnalysisReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.JobRecommendationBatch;
import com.internpilot.entity.JobRecommendationItem;
import com.internpilot.entity.Resume;
import com.internpilot.entity.ResumeVersion;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.AnalysisReportMapper;
import com.internpilot.mapper.ApplicationRecordMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.JobRecommendationBatchMapper;
import com.internpilot.mapper.JobRecommendationItemMapper;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.mapper.ResumeVersionMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.recommendation.impl.JobRecommendationServiceImpl;
import com.internpilot.vo.recommendation.JobRecommendationGenerateResponse;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRecommendationServiceGenerateCoverageTest {

    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeVersionMapper resumeVersionMapper;
    @Mock
    private JobDescriptionMapper jobDescriptionMapper;
    @Mock
    private AnalysisReportMapper analysisReportMapper;
    @Mock
    private ApplicationRecordMapper applicationRecordMapper;
    @Mock
    private JobRecommendationBatchMapper batchMapper;
    @Mock
    private JobRecommendationItemMapper itemMapper;

    private JobRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JobRecommendationServiceImpl(
                resumeMapper,
                resumeVersionMapper,
                jobDescriptionMapper,
                analysisReportMapper,
                applicationRecordMapper,
                batchMapper,
                itemMapper
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generateShouldScoreSortFilterAppliedJobsAndPersistItems() {
        mockLoginUser(8L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume("resume text Java Spring Boot backend Redis"));
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildVersion("target backend Java MySQL Redis"));
        when(jobDescriptionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                buildJob(21L, "Java Backend", "backend", "Java Spring Boot Redis MySQL"),
                buildJob(22L, "Frontend", "frontend", "Vue CSS"),
                buildJob(23L, "Backend Applied", "backend", "Java Redis Docker")
        ));
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class)))
                .thenReturn(0L, 0L, 1L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildReport(101L, 21L, 95), null, buildReport(103L, 23L, 88));
        doAnswer(invocation -> {
            JobRecommendationBatch batch = invocation.getArgument(0);
            batch.setId(700L);
            batch.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(batchMapper).insert(any(JobRecommendationBatch.class));

        JobRecommendationGenerateRequest request = new JobRecommendationGenerateRequest();
        request.setResumeId(10L);
        request.setLimit(50);
        request.setIncludeApplied(false);

        JobRecommendationGenerateResponse response = service.generate(request);

        assertThat(response.getBatchId()).isEqualTo(700L);
        assertThat(response.getRecommendedCount()).isEqualTo(2);
        verify(batchMapper).updateById(any(JobRecommendationBatch.class));

        ArgumentCaptor<JobRecommendationItem> itemCaptor = ArgumentCaptor.forClass(JobRecommendationItem.class);
        verify(itemMapper, org.mockito.Mockito.times(2)).insert(itemCaptor.capture());
        assertThat(itemCaptor.getAllValues())
                .extracting(JobRecommendationItem::getJobId)
                .doesNotContain(23L);
        assertThat(itemCaptor.getAllValues().get(0).getRecommendationScore())
                .isGreaterThanOrEqualTo(itemCaptor.getAllValues().get(1).getRecommendationScore());
        assertThat(itemCaptor.getAllValues().get(0).getMatchedSkills()).contains("Java");
        assertThat(itemCaptor.getAllValues().get(0).getReasons()).contains("95");
    }

    @Test
    void generateShouldIncludeAppliedWhenRequestedAndClampLimit() {
        mockLoginUser(8L);
        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume("backend Java Redis"));
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(jobDescriptionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
                buildJob(21L, "Java Backend", "backend", "Java Redis"),
                buildJob(22L, "Another Backend", "backend", "Spring Boot MySQL")
        ));
        when(applicationRecordMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L, 0L);
        when(analysisReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        doAnswer(invocation -> {
            JobRecommendationBatch batch = invocation.getArgument(0);
            batch.setId(701L);
            return 1;
        }).when(batchMapper).insert(any(JobRecommendationBatch.class));

        JobRecommendationGenerateRequest request = new JobRecommendationGenerateRequest();
        request.setResumeId(10L);
        request.setLimit(0);
        request.setIncludeApplied(true);

        JobRecommendationGenerateResponse response = service.generate(request);

        assertThat(response.getRecommendedCount()).isEqualTo(2);
        verify(itemMapper, org.mockito.Mockito.times(2)).insert(any(JobRecommendationItem.class));
    }

    @Test
    void generateShouldRejectMissingResumeEmptyResumeTextAndNoJobs() {
        mockLoginUser(8L);
        JobRecommendationGenerateRequest request = new JobRecommendationGenerateRequest();
        request.setResumeId(10L);

        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(BusinessException.class);

        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume(""));
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(BusinessException.class);

        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildResume("Java"));
        when(resumeVersionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(jobDescriptionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(request)).isInstanceOf(BusinessException.class);
    }

    private Resume buildResume(String parsedText) {
        Resume resume = new Resume();
        resume.setId(10L);
        resume.setUserId(8L);
        resume.setResumeName("backend resume");
        resume.setOriginalFileName("resume.pdf");
        resume.setParsedText(parsedText);
        return resume;
    }

    private ResumeVersion buildVersion(String content) {
        ResumeVersion version = new ResumeVersion();
        version.setId(11L);
        version.setResumeId(10L);
        version.setUserId(8L);
        version.setVersionName("optimized");
        version.setContent(content);
        return version;
    }

    private JobDescription buildJob(Long id, String title, String type, String content) {
        JobDescription job = new JobDescription();
        job.setId(id);
        job.setUserId(8L);
        job.setCompanyName("ACME");
        job.setJobTitle(title);
        job.setJobType(type);
        job.setJdContent(content);
        job.setSkillRequirements(content);
        job.setLocation("Remote");
        job.setSalaryRange("100-200/day");
        job.setSourcePlatform("site");
        return job;
    }

    private AnalysisReport buildReport(Long id, Long jobId, Integer score) {
        AnalysisReport report = new AnalysisReport();
        report.setId(id);
        report.setUserId(8L);
        report.setResumeId(10L);
        report.setResumeVersionId(11L);
        report.setJobId(jobId);
        report.setMatchScore(score);
        return report;
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}
