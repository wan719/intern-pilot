package com.internpilot.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "生成AI面试题请求")
public class InterviewQuestionGenerateRequest {

    @Schema(description = "简历ID", example = "1")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    @Schema(description = "岗位ID", example = "1")
    @NotNull(message = "岗位ID不能为空")
    private Long jobId;

    @Schema(description = "简历版本ID，不传则使用当前版本")
    private Long resumeVersionId;

    @Schema(description = "AI分析报告ID", example = "1")
    private Long analysisReportId;

    @Schema(description = "题目数量，默认8", example = "8")
    private Integer questionCount;

    @Schema(description = "题目分类列表，不传则生成所有类型")
    private List<String> categories;

    @Schema(description = "难度列表，不传则混合难度")
    private List<String> difficulties;

    @Schema(description = "是否生成参考答案", example = "true")
    private Boolean includeAnswer;

    @Schema(description = "是否生成追问问题", example = "true")
    private Boolean includeFollowUps;

    @Schema(description = "是否强制重新生成", example = "false")
    private Boolean forceRefresh = false;
}
