package com.internpilot.dto.application;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ApplicationFromReportRequest {
    @NotNull(message = "报告 ID 不能为空")
    @Positive(message = "报告 ID 必须为正整数")
    private Long reportId;
    private String note;
    private String priority;

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("不允许提交字段: " + name);
    }
}
