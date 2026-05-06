package com.internpilot.vo.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "岗位列表响应")
public class JobListResponse {

    @Schema(description = "岗位ID")
    private Long jobId;

    @Schema(description = "公司名称")
    private String companyName;

    @Schema(description = "岗位名称")
    private String jobTitle;

    @Schema(description = "岗位类型")
    private String jobType;

    @Schema(description = "工作地点")
    private String location;

    @Schema(description = "来源平台")
    private String sourcePlatform;

    @Schema(description = "薪资范围")
    private String salaryRange;

    @Schema(description = "每周工作天数")
    private String workDaysPerWeek;

    @Schema(description = "实习周期")
    private String internshipDuration;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
