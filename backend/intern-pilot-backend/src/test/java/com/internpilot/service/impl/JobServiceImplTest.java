package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.dto.job.JobCreateRequest;
import com.internpilot.dto.job.JobUpdateRequest;
import com.internpilot.entity.JobDescription;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.job.JobCreateResponse;
import com.internpilot.vo.job.JobDetailResponse;
import com.internpilot.vo.job.JobListResponse;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @InjectMocks
    private JobServiceImpl jobService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldBindCurrentUser() {
        mockLoginUser(1L);

        JobCreateRequest request = new JobCreateRequest();
        request.setCompanyName("腾讯");
        request.setJobTitle("Java后端开发实习生");
        request.setJobType("Java后端");
        request.setLocation("深圳");
        request.setSourcePlatform("Boss直聘");
        request.setJobUrl("https://example.com/job/123");
        request.setJdContent("要求熟悉 Java、Spring Boot、MySQL、Redis");
        request.setSkillRequirements("Java, Spring Boot");
        request.setSalaryRange("200-400元/天");
        request.setWorkDaysPerWeek("5天/周");
        request.setInternshipDuration("3个月");

        doAnswer(invocation -> {
            JobDescription job = invocation.getArgument(0);
            job.setId(11L);
            job.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(jobDescriptionMapper).insert(any(JobDescription.class));

        JobCreateResponse response = jobService.create(request);

        assertEquals(11L, response.getJobId());
        assertEquals("腾讯", response.getCompanyName());

        ArgumentCaptor<JobDescription> captor = ArgumentCaptor.forClass(JobDescription.class);
        verify(jobDescriptionMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    void listShouldMapPagedJobs() {
        mockLoginUser(1L);

        JobDescription job = new JobDescription();
        job.setId(5L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        job.setJobType("Java后端");
        job.setLocation("深圳");
        job.setSourcePlatform("Boss直聘");
        job.setSalaryRange("200-400元/天");
        job.setWorkDaysPerWeek("5天/周");
        job.setInternshipDuration("3个月");
        job.setCreatedAt(LocalDateTime.now());

        Page<JobDescription> page = new Page<>(1, 10);
        page.setRecords(List.of(job));
        page.setTotal(1L);
        page.setPages(1L);

        when(jobDescriptionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = jobService.list("Java", "Java后端", "深圳", 1, 10);

        assertEquals(1L, result.getTotal());
        JobListResponse first = result.getRecords().get(0);
        assertEquals("腾讯", first.getCompanyName());
        assertEquals("Java后端开发实习生", first.getJobTitle());
    }

    @Test
    void getDetailShouldReturnOwnedJob() {
        mockLoginUser(1L);

        JobDescription job = new JobDescription();
        job.setId(9L);
        job.setUserId(1L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        job.setJobType("Java后端");
        job.setLocation("深圳");
        job.setSourcePlatform("Boss直聘");
        job.setJobUrl("https://example.com/job/123");
        job.setJdContent("JD");
        job.setSkillRequirements("Java");
        job.setSalaryRange("200-400元/天");
        job.setWorkDaysPerWeek("5天/周");
        job.setInternshipDuration("3个月");
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());

        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        JobDetailResponse response = jobService.getDetail(9L);

        assertEquals("腾讯", response.getCompanyName());
        assertEquals("JD", response.getJdContent());
    }

    @Test
    void deleteShouldMarkDeleted() {
        mockLoginUser(1L);

        JobDescription job = new JobDescription();
        job.setId(7L);
        job.setUserId(1L);

        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        Boolean result = jobService.delete(7L);

        assertTrue(result);
        verify(jobDescriptionMapper).deleteById(7L);
    }

    @Test
    void updateShouldApplyNewFields() {
        mockLoginUser(1L);

        JobDescription job = new JobDescription();
        job.setId(12L);
        job.setUserId(1L);
        when(jobDescriptionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(job);

        JobUpdateRequest request = new JobUpdateRequest();
        request.setCompanyName("阿里");
        request.setJobTitle("后端开发实习生");
        request.setJobType("Java后端");
        request.setLocation("杭州");
        request.setSourcePlatform("官网");
        request.setJobUrl("https://example.com/job/456");
        request.setJdContent("新的 JD");
        request.setSkillRequirements("Java, Redis");
        request.setSalaryRange("250-350元/天");
        request.setWorkDaysPerWeek("4天/周");
        request.setInternshipDuration("4个月");

        Boolean result = jobService.update(12L, request);

        assertTrue(result);
        assertEquals("阿里", job.getCompanyName());
        assertEquals("新的 JD", job.getJdContent());
        assertFalse(job.getJobUrl().isBlank());
        verify(jobDescriptionMapper).updateById(job);
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "wan", "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
