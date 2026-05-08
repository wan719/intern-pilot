package com.internpilot.vo.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建AI分析任务响应")
public class AnalysisTaskCreateResponse {

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "当前进度")
    private Integer progress;

    @Schema(description = "提示信息")
    private String message;
}