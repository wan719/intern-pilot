package com.internpilot.dto.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建反馈请求")
public class FeedbackCreateRequest {

    @NotBlank(message = "反馈类型不能为空")
    @Size(max = 30, message = "反馈类型长度不能超过 30 个字符")
    @Schema(description = "反馈类型", example = "BUG")
    private String type;

    @NotBlank(message = "反馈标题不能为空")
    @Size(max = 100, message = "反馈标题长度不能超过 100 个字符")
    @Schema(description = "反馈标题", example = "页面加载缓慢")
    private String title;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 3000, message = "反馈内容长度不能超过 3000 个字符")
    @Schema(description = "反馈内容", example = "首页加载时间超过5秒")
    private String content;

    @Size(max = 100, message = "联系方式长度不能超过 100 个字符")
    @Schema(description = "联系方式", example = "test@example.com")
    private String contact;

    @Schema(description = "是否允许联系", example = "true")
    private Boolean allowContact;

    @Size(max = 500, message = "反馈页面长度不能超过 500 个字符")
    @Schema(description = "反馈页面", example = "/dashboard")
    private String pageUrl;

    @Size(max = 500, message = "浏览器信息长度不能超过 500 个字符")
    @Schema(description = "浏览器信息")
    private String browserInfo;
}
