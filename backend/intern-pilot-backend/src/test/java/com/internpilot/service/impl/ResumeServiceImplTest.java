package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.entity.Resume;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.security.CustomUserDetails;
import com.internpilot.service.FileStorageService;
import com.internpilot.service.ResumeParseService;
import com.internpilot.service.StoredFileInfo;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ResumeParseService resumeParseService;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadShouldAutoSetFirstResumeAsDefault() {
        mockLoginUser(1L);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.pdf",
                "application/pdf",
                "pdf".getBytes(StandardCharsets.UTF_8)
        );

        StoredFileInfo storedFileInfo = new StoredFileInfo(
                "resume.pdf",
                "1_20260506201030_abcd1234.pdf",
                "uploads/resumes/user-1/1_20260506201030_abcd1234.pdf",
                "PDF",
                2048L
        );

        when(fileStorageService.store(file, 1L)).thenReturn(storedFileInfo);
        when(resumeParseService.parse(file, "PDF")).thenReturn("Java Spring Boot Redis");
        when(resumeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        doAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(10L);
            resume.setCreatedAt(LocalDateTime.now());
            return 1;
        }).when(resumeMapper).insert(any(Resume.class));

        ResumeUploadResponse response = resumeService.upload(file, "Java后端简历");

        assertEquals(10L, response.getResumeId());
        assertEquals("Java后端简历", response.getResumeName());
        assertEquals("PDF", response.getFileType());
        assertTrue(response.getIsDefault());
        assertTrue(response.getParsedTextPreview().contains("Java Spring Boot"));

        ArgumentCaptor<Resume> captor = ArgumentCaptor.forClass(Resume.class);
        verify(resumeMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
    }

    @Test
    void getDetailShouldReturnOwnedResume() {
        mockLoginUser(1L);

        Resume resume = new Resume();
        resume.setId(2L);
        resume.setUserId(1L);
        resume.setResumeName("测试简历");
        resume.setOriginalFileName("resume.docx");
        resume.setStoredFileName("stored.docx");
        resume.setFilePath("uploads/resumes/user-1/stored.docx");
        resume.setFileType("DOCX");
        resume.setFileSize(1234L);
        resume.setParseStatus("SUCCESS");
        resume.setIsDefault(1);
        resume.setParsedText("full parsed text");
        resume.setCreatedAt(LocalDateTime.now());
        resume.setUpdatedAt(LocalDateTime.now());

        when(resumeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(resume);

        ResumeDetailResponse response = resumeService.getDetail(2L);

        assertEquals("测试简历", response.getResumeName());
        assertEquals("full parsed text", response.getParsedText());
        assertTrue(response.getIsDefault());
    }

    @Test
    void listShouldMapPageResult() {
        mockLoginUser(1L);

        Resume resume = new Resume();
        resume.setId(3L);
        resume.setResumeName("A");
        resume.setOriginalFileName("a.pdf");
        resume.setFileType("PDF");
        resume.setFileSize(100L);
        resume.setParseStatus("SUCCESS");
        resume.setIsDefault(0);
        resume.setCreatedAt(LocalDateTime.now());

        Page<Resume> page = new Page<>(1, 10);
        page.setRecords(List.of(resume));
        page.setTotal(1L);
        page.setPages(1L);

        when(resumeMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        var result = resumeService.list(1, 10);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertFalse(result.getRecords().get(0).getIsDefault());
    }

    private void mockLoginUser(Long userId) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, "wan", "USER");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
