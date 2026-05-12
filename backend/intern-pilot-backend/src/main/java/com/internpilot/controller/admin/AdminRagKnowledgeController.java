package com.internpilot.controller.admin;

import com.internpilot.annotation.OperationLog;
import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.dto.rag.RagKnowledgeCreateRequest;
import com.internpilot.dto.rag.RagKnowledgeUpdateRequest;
import com.internpilot.dto.rag.RagSearchRequest;
import com.internpilot.enums.OperationTypeEnum;
import com.internpilot.service.RagKnowledgeService;
import com.internpilot.vo.rag.RagKnowledgeDetailResponse;
import com.internpilot.vo.rag.RagKnowledgeListResponse;
import com.internpilot.vo.rag.RagSearchResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rag/knowledge")
@RequiredArgsConstructor
public class AdminRagKnowledgeController {

    private final RagKnowledgeService ragKnowledgeService;

    @Operation(summary = "创建RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "创建知识文档", type = OperationTypeEnum.CREATE, recordParams = false)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid RagKnowledgeCreateRequest request) {
        return Result.success(ragKnowledgeService.create(request));
    }

    @Operation(summary = "修改RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "修改知识文档", type = OperationTypeEnum.UPDATE, recordParams = false)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PutMapping("/{documentId}")
    public Result<Boolean> update(
            @PathVariable Long documentId,
            @RequestBody @Valid RagKnowledgeUpdateRequest request
    ) {
        return Result.success(ragKnowledgeService.update(documentId, request));
    }

    @Operation(summary = "删除RAG知识文档")
    @OperationLog(module = "RAG知识库", operation = "删除知识文档", type = OperationTypeEnum.DELETE)
    @PreAuthorize("hasAuthority('rag:knowledge:delete')")
    @DeleteMapping("/{documentId}")
    public Result<Boolean> delete(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.delete(documentId));
    }

    @Operation(summary = "重建知识文档切片和向量")
    @OperationLog(module = "RAG知识库", operation = "重建知识向量", type = OperationTypeEnum.UPDATE)
    @PreAuthorize("hasAuthority('rag:knowledge:write')")
    @PostMapping("/{documentId}/rebuild")
    public Result<Boolean> rebuild(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.rebuildChunks(documentId));
    }

    @Operation(summary = "查询RAG知识文档列表")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @GetMapping
    public Result<PageResult<RagKnowledgeListResponse>> list(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String knowledgeType,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(
                ragKnowledgeService.list(direction, knowledgeType, enabled, pageNum, pageSize)
        );
    }

    @Operation(summary = "查询RAG知识文档详情")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @GetMapping("/{documentId}")
    public Result<RagKnowledgeDetailResponse> getDetail(@PathVariable Long documentId) {
        return Result.success(ragKnowledgeService.getDetail(documentId));
    }

    @Operation(summary = "测试RAG知识检索")
    @PreAuthorize("hasAuthority('rag:knowledge:read')")
    @PostMapping("/search")
    public Result<List<RagSearchResultResponse>> search(@RequestBody @Valid RagSearchRequest request) {
        return Result.success(ragKnowledgeService.search(request));
    }
}
