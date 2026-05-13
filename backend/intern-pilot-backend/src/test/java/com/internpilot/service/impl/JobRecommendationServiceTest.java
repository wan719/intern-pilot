package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.JobDescription;
import com.internpilot.entity.JobRecommendationBatch;
import com.internpilot.entity.JobRecommendationItem;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.JobDescriptionMapper;
import com.internpilot.mapper.JobRecommendationBatchMapper;
import com.internpilot.mapper.JobRecommendationItemMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.vo.recommendation.JobRecommendationBatchDetailResponse;
import com.internpilot.vo.recommendation.JobRecommendationBatchListResponse;
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
class JobRecommendationServiceTest {

    @Mock
    private JobRecommendationBatchMapper batchMapper;

    @Mock
    private JobRecommendationItemMapper itemMapper;

    @Mock
    private JobDescriptionMapper jobDescriptionMapper;

    @InjectMocks
    private JobRecommendationServiceImpl jobRecommendationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listShouldReturnPagedBatches() {
        mockLoginUser(1L);

        JobRecommendationBatch batch = new JobRecommendationBatch();
        batch.setId(1L);
        batch.setResumeId(10L);
        batch.setTitle("我的简历 - 岗位推荐");
        batch.setJobCount(5);
        batch.setRecommendedCount(3);
        batch.setStrategy("RULE_BASED");
        batch.setCreatedAt(LocalDateTime.now());

        Page<JobRecommendationBatch> page = new Page<>(1, 10);
        page.setRecords(List.of(batch));
        page.setTotal(1L);

        when(batchMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<JobRecommendationBatchListResponse> result = jobRecommendationService.list(1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        assertEquals("我的简历 - 岗位推荐", result.getRecords().get(0).getTitle());
        assertEquals(5, result.getRecords().get(0).getJobCount());
    }

    @Test
    void getDetailShouldReturnFullDetail() {
        mockLoginUser(1L);

        JobRecommendationBatch batch = new JobRecommendationBatch();
        batch.setId(1L);
        batch.setUserId(1L);
        batch.setResumeId(10L);
        batch.setTitle("我的简历 - 岗位推荐");
        batch.setJobCount(5);
        batch.setRecommendedCount(3);
        batch.setStrategy("RULE_BASED");
        batch.setCreatedAt(LocalDateTime.now());

        when(batchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(batch);

        JobRecommendationItem item = new JobRecommendationItem();
        item.setId(1L);
        item.setJobId(20L);
        item.setRecommendationScore(85);
        item.setRecommendationLevel("HIGH");
        item.setSortOrder(1);

        when(itemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

        JobDescription job = new JobDescription();
        job.setId(20L);
        job.setCompanyName("腾讯");
        job.setJobTitle("Java后端开发实习生");
        job.setJobType("Java后端");
        job.setLocation("深圳");
        when(jobDescriptionMapper.selectById(20L)).thenReturn(job);

        JobRecommendationBatchDetailResponse response = jobRecommendationService.getDetail(1L);

        assertNotNull(response);
        assertEquals("我的简历 - 岗位推荐", response.getTitle());
        assertEquals(1, response.getItems().size());
        assertEquals("腾讯", response.getItems().get(0).getCompanyName());
    }

    @Test
    void getDetailShouldThrowExceptionWhenNotFound() {
        mockLoginUser(1L);

        when(batchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> {
            jobRecommendationService.getDetail(999L);
        });
    }

    @Test
    void deleteShouldSoftDeleteBatchAndItems() {
        mockLoginUser(1L);

        JobRecommendationBatch batch = new JobRecommendationBatch();
        batch.setId(1L);
        batch.setUserId(1L);

        when(batchMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(batch);

        Boolean result = jobRecommendationService.delete(1L);

        assertTrue(result);
        verify(itemMapper).delete(any(LambdaQueryWrapper.class));
        verify(batchMapper).deleteById(1L);
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "testuser", "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }
}