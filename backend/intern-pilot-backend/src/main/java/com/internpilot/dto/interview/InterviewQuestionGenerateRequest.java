package com.internpilot.dto.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "题目数量", example = "8")
    @Min(value = 1, message = "面试题数量不能少于 1")
    @Max(value = 30, message = "面试题数量不能超过 30")
    private Integer questionCount;

    @Schema(description = "题目分类列表")
    @Size(max = 10, message = "面试题分类数量不能超过 10 个")
    private List<String> categories;

    @Schema(description = "难度列表")
    @Size(max = 5, message = "面试题难度数量不能超过 5 个")
    private List<String> difficulties;

    @Schema(description = "是否生成参考答案", example = "true")
    private Boolean includeAnswer;

    @Schema(description = "是否生成追问问题", example = "true")
    private Boolean includeFollowUps;
}
