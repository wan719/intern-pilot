package com.internpilot.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@Schema(description = "修改岗位请求")
public class JobUpdateRequest {

    @Schema(description = "公司名称", example = "腾讯")
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    @Schema(description = "岗位名称", example = "Java后端开发实习生")
    @NotBlank(message = "岗位名称不能为空")
    private String jobTitle;

    @Schema(description = "岗位类型", example = "Java后端")
    private String jobType;

    @Schema(description = "工作地点", example = "深圳")
    private String location;

    @Schema(description = "来源平台", example = "Boss直聘")
    private String sourcePlatform;

    @Schema(description = "岗位链接", example = "https://example.com/job/123")
    @URL(message = "岗位链接格式不正确")
    private String jobUrl;

    @Schema(description = "岗位 JD 原文")
    @NotBlank(message = "岗位 JD 不能为空")
    private String jdContent;

    @Schema(description = "技能要求", example = "Java, Spring Boot, MySQL, Redis")
    private String skillRequirements;

    @Schema(description = "薪资范围", example = "200-400元/天")
    private String salaryRange;

    @Schema(description = "每周工作天数", example = "5天/周")
    private String workDaysPerWeek;

    @Schema(description = "实习周期", example = "3个月")
    private String internshipDuration;
}
