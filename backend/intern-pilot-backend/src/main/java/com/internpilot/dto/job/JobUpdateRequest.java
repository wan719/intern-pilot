package com.internpilot.dto.job;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改岗位请求")
public class JobUpdateRequest {

    @Schema(description = "公司名称", example = "腾讯")
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称长度不能超过 100 个字符")
    private String companyName;

    @Schema(description = "岗位名称", example = "Java后端开发实习生")
    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 100, message = "岗位名称长度不能超过 100 个字符")
    private String jobTitle;

    @Schema(description = "岗位类型", example = "Java后端")
    @Size(max = 50, message = "岗位类型长度不能超过 50 个字符")
    private String jobType;

    @Schema(description = "工作地点", example = "深圳")
    @Size(max = 100, message = "工作地点长度不能超过 100 个字符")
    private String location;

    @Schema(description = "来源平台", example = "Boss直聘")
    @Size(max = 100, message = "来源平台长度不能超过 100 个字符")
    private String sourcePlatform;

    @Schema(description = "岗位链接", example = "https://example.com/job/123")
    @Pattern(regexp = "^$|https?://.+", message = "岗位链接必须以 http:// 或 https:// 开头")
    @Size(max = 500, message = "岗位链接长度不能超过 500 个字符")
    private String jobUrl;

    @Schema(description = "岗位 JD 原文")
    @NotBlank(message = "岗位 JD 不能为空")
    @Size(max = 20000, message = "岗位 JD 长度不能超过 20000 个字符")
    private String jdContent;

    @Schema(description = "技能要求", example = "Java, Spring Boot, MySQL, Redis")
    @Size(max = 2000, message = "技能要求长度不能超过 2000 个字符")
    private String skillRequirements;

    @Schema(description = "薪资范围", example = "200-400元/天")
    @Size(max = 100, message = "薪资范围长度不能超过 100 个字符")
    private String salaryRange;

    @Schema(description = "每周工作天数", example = "5天/周")
    @Size(max = 50, message = "每周工作天数长度不能超过 50 个字符")
    private String workDaysPerWeek;

    @Schema(description = "实习周期", example = "3个月")
    @Size(max = 100, message = "实习周期长度不能超过 100 个字符")
    private String internshipDuration;
}
