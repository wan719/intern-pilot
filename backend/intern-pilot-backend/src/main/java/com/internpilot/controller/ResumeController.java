package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.service.ResumeService;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "简历管理接口")
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传简历", description = "上传 PDF 或 DOCX 简历，并解析文本内容")
    @PostMapping("/upload")
    public Result<ResumeUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resumeName", required = false) String resumeName
    ) {
        return Result.success(resumeService.upload(file, resumeName));
    }

    @Operation(summary = "查询简历列表", description = "分页查询当前用户上传的简历列表")
    @GetMapping
    public Result<PageResult<ResumeListResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(resumeService.list(pageNum, pageSize));
    }

    @Operation(summary = "查询简历详情", description = "查询当前用户某份简历的详细信息和解析文本")
    @GetMapping("/{id}")
    public Result<ResumeDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(resumeService.getDetail(id));
    }

    @Operation(summary = "删除简历", description = "逻辑删除当前用户的简历")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(resumeService.delete(id));
    }

    @Operation(summary = "设置默认简历", description = "将当前用户某份简历设置为默认简历")
    @PutMapping("/{id}/default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        return Result.success(resumeService.setDefault(id));
    }
}
