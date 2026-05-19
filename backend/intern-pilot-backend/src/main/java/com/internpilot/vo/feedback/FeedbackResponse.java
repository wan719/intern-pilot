package com.internpilot.vo.feedback;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "反馈响应")
public class FeedbackResponse {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户邮箱")
    private String userEmail;

    @Schema(description = "反馈类型")
    private String type;

    @Schema(description = "反馈类型描述")
    private String typeDescription;

    @Schema(description = "反馈标题")
    private String title;

    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "反馈页面")
    private String pageUrl;

    @Schema(description = "联系方式")
    private String contact;

    @Schema(description = "是否允许联系")
    private Boolean allowContact;

    @Schema(description = "浏览器信息")
    private String browserInfo;

    @Schema(description = "反馈状态")
    private String status;

    @Schema(description = "反馈状态描述")
    private String statusDescription;

    @Schema(description = "管理员回复")
    private String adminReply;

    @Schema(description = "处理人ID")
    private Long handledBy;

    @Schema(description = "处理人名称")
    private String handledByName;

    @Schema(description = "处理时间")
    private LocalDateTime handledAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}