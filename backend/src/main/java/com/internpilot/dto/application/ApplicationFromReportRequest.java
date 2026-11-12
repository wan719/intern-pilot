package com.internpilot.dto.application;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Data
public class ApplicationFromReportRequest {
    @NotNull(message = "报告 ID 不能为空")
    @Positive(message = "报告 ID 必须为正整数")
    @JsonDeserialize(using = StrictReportIdDeserializer.class)
    private Long reportId;
    private String note;
    private String priority;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private transient boolean reportIdSeen;

    @JsonSetter("reportId")
    public void setReportId(Long reportId) {
        if (reportIdSeen) {
            throw new IllegalArgumentException("报告 ID 不可重复");
        }
        reportIdSeen = true;
        this.reportId = reportId;
    }

    @JsonAnySetter
    public void rejectUnknownField(String name, Object value) {
        throw new IllegalArgumentException("不允许提交字段: " + name);
    }

    public static class StrictReportIdDeserializer extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                throw JsonMappingException.from(parser, "报告 ID 必须为 JSON 整数");
            }
            return parser.getLongValue();
        }
    }
}
