package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.InterviewQuestion;
import com.internpilot.entity.InterviewQuestionReport;
import com.internpilot.entity.JobDescription;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.InterviewQuestionMapper;
import com.internpilot.mapper.InterviewQuestionReportMapper;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.interview.InterviewQuestionDetailResponse;
import com.internpilot.vo.interview.InterviewQuestionListResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterviewQuestionServiceTest {

    @Mock
    private InterviewQuestionReportMapper interviewQuestionReportMapper;

    @Mock
    private InterviewQuestionMapper interviewQuestionMapper;

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @InjectMocks
    private InterviewQuestionServiceImpl interviewQuestionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnPagedReports() {
        mockLoginUser(1L);

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setId(1L);
        report.setTitle("腾讯 Java后端 面试题准备");
        report.setResumeId(10L);
        report.setJobId(20L);
        report.setQuestionCount(5);
        report.setCreatedAt(LocalDateTime.now());

        Page<InterviewQuestionReport> page = new Page<>(1, 10);
        page.setRecords(List.of(report));
        page.setTotal(1L);

        when(interviewQuestionReportMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(page);

        JobDescription job = new JobDescription();
        job.setId(20L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        when(jobDescriptionMapper.selectById(20L)).thenReturn(job);

        PageResult<InterviewQuestionListResponse> result = interviewQuestionService.list(null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("腾讯", result.getRecords().get(0).getCompanyName());
        assertEquals("Java后端开发实习生", result.getRecords().get(0).getJobTitle());
    }

    @Test
    void getDetailShouldReturnFullDetail() {
        mockLoginUser(1L);

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setId(1L);
        report.setUserId(1L);
        report.setTitle("腾讯 Java后端 面试题准备");
        report.setResumeId(10L);
        report.setJobId(20L);
        report.setQuestionCount(2);
        report.setCreatedAt(LocalDateTime.now());

        when(interviewQuestionReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);

        InterviewQuestion q1 = new InterviewQuestion();
        q1.setId(1L);
        q1.setQuestionType("JOB_SKILL");
        q1.setDifficulty("MEDIUM");
        q1.setQuestion("请介绍Spring Boot自动配置原理");
        q1.setAnswer("Spring Boot通过@EnableAutoConfiguration...");
        q1.setAnswerPoints("[\"自动配置原理\",\"@Conditional注解\"]");
        q1.setRelatedSkills("[\"Spring Boot\"]");
        q1.setFollowUps("[\"追问1\",\"追问2\"]");
        q1.setKeywords("[\"Spring Boot\",\"自动配置\"]");
        q1.setSource("根据岗位要求生成");
        q1.setSortOrder(1);

        InterviewQuestion q2 = new InterviewQuestion();
        q2.setId(2L);
        q2.setQuestionType("PROJECT_EXPERIENCE");
        q2.setDifficulty("HARD");
        q2.setQuestion("请介绍你最有挑战的项目");
        q2.setAnswer("我参与了一个...");
        q2.setAnswerPoints("[\"项目难点\",\"解决方案\"]");
        q2.setRelatedSkills("[\"Java\"]");
        q2.setSortOrder(2);

        when(interviewQuestionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(q1, q2));

        JobDescription job = new JobDescription();
        job.setId(20L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        when(jobDescriptionMapper.selectById(20L)).thenReturn(job);

        InterviewQuestionDetailResponse response = interviewQuestionService.getDetail(1L);

        assertNotNull(response);
        assertEquals("腾讯", response.getCompanyName());
        assertEquals(2, response.getQuestions().size());
    }

    @Test
    void getDetailShouldThrowExceptionWhenNotFound() {
        mockLoginUser(1L);

        when(interviewQuestionReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            interviewQuestionService.getDetail(999L);
        });
    }

    @Test
    void deleteShouldSoftDeleteReportAndQuestions() {
        mockLoginUser(1L);

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setId(1L);
        report.setUserId(1L);

        when(interviewQuestionReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);
        when(interviewQuestionReportMapper.updateById(any(InterviewQuestionReport.class))).thenReturn(1);

        InterviewQuestion q1 = new InterviewQuestion();
        q1.setId(1L);
        when(interviewQuestionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(q1));
        when(interviewQuestionMapper.updateById(any(InterviewQuestion.class))).thenReturn(1);

        Boolean result = interviewQuestionService.delete(1L);

        assertTrue(result);
        verify(interviewQuestionReportMapper).updateById(any(InterviewQuestionReport.class));
        verify(interviewQuestionMapper).updateById(any(InterviewQuestion.class));
    }

    @Test
    void getDetailShouldReturnNewFields() {
        mockLoginUser(1L);

        InterviewQuestionReport report = new InterviewQuestionReport();
        report.setId(1L);
        report.setUserId(1L);
        report.setTitle("测试面试题");
        report.setResumeId(10L);
        report.setJobId(20L);
        report.setQuestionCount(1);
        report.setCreatedAt(LocalDateTime.now());

        when(interviewQuestionReportMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(report);

        InterviewQuestion q1 = new InterviewQuestion();
        q1.setId(1L);
        q1.setQuestionType("JAVA_BASIC");
        q1.setDifficulty("EASY");
        q1.setQuestion("测试问题");
        q1.setAnswer("测试答案");
        q1.setAnswerPoints("[\"要点1\"]");
        q1.setRelatedSkills("[\"Java\"]");
        q1.setFollowUps("[\"追问1\",\"追问2\"]");
        q1.setKeywords("[\"Java\",\"OOP\"]");
        q1.setSource("根据岗位要求生成");
        q1.setSortOrder(1);

        when(interviewQuestionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(q1));

        JobDescription job = new JobDescription();
        job.setId(20L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        when(jobDescriptionMapper.selectById(20L)).thenReturn(job);

        InterviewQuestionDetailResponse response = interviewQuestionService.getDetail(1L);

        assertNotNull(response);
        assertEquals(1, response.getQuestions().size());
        assertNotNull(response.getQuestions().get(0).getFollowUps());
        assertEquals(2, response.getQuestions().get(0).getFollowUps().size());
        assertNotNull(response.getQuestions().get(0).getKeywords());
        assertEquals(2, response.getQuestions().get(0).getKeywords().size());
        assertEquals("根据岗位要求生成", response.getQuestions().get(0).getSource());
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}