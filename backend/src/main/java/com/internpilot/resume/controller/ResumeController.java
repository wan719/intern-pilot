package com.internpilot.resume.controller;

import com.internpilot.annotation.LogExecutionTime;
import com.internpilot.audit.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.audit.enums.OperationTypeEnum;
import com.internpilot.resume.service.ResumeOriginalFile;
import com.internpilot.resume.service.ResumeService;
import com.internpilot.resume.vo.ResumeDetailResponse;
import com.internpilot.resume.vo.ResumeListResponse;
import com.internpilot.resume.vo.ResumeUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@Tag(name = "简历管理接")
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传简", description = "上传 PDF DOCX 简历，并解析文本内")
    @OperationLog(module = "简历管", operation = "上传简", type = OperationTypeEnum.UPLOAD, recordParams = false)
    @LogExecutionTime("简历上传解析")
    @PreAuthorize("hasAuthority('resume:write')")
    @PostMapping("/upload")
    public Result<ResumeUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resumeName", required = false) String resumeName) {
        return Result.success(resumeService.upload(file, resumeName));
    }

    @Operation(summary = "查询简历列", description = "分页查询当前用户上传的简历列")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping
    public Result<PageResult<ResumeListResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(resumeService.list(pageNum, pageSize));
    }

    @Operation(summary = "查询简历详", description = "查询当前用户某份简历的详细信息和解析文")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/{id}")
    public Result<ResumeDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(resumeService.getDetail(id));
    }

    @Operation(summary = "读取本人上传的简历原件")
    @PreAuthorize("hasAuthority('resume:read')")
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getOriginal(@PathVariable Long id) {
        return resumeService.findOriginal(id)
                .map(this::originalResponse)
                .orElseGet(() -> ResponseEntity.notFound().header(HttpHeaders.CACHE_CONTROL, "private, no-store").build());
    }

    private ResponseEntity<Resource> originalResponse(ResumeOriginalFile original) {
        MediaType mediaType = switch (original.type()) {
            case PDF -> MediaType.APPLICATION_PDF;
            case DOCX -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        };
        String name = safeDownloadName(original.originalName(), original.type().getExtension());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(original.path()));
    }

    private String safeDownloadName(String name, String extension) {
        if (name != null) {
            String basename = name.replace('\\', '/');
            basename = basename.substring(basename.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
            if (!basename.isEmpty() && !basename.equals(".") && !basename.equals("..")) {
                return basename;
            }
        }
        return "resume." + extension;
    }

    @Operation(summary = "删除简", description = "逻辑删除当前用户的简")
    @OperationLog(module = "简历管", operation = "删除简", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('resume:delete')")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(resumeService.delete(id));
    }

    @Operation(summary = "设置默认简", description = "将当前用户某份简历设置为默认简")
    @OperationLog(module = "简历管", operation = "设置默认简", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('resume:write')")
    @PutMapping("/{id}/default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        return Result.success(resumeService.setDefault(id));
    }
}
