package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "简历版本对比响应")
public class ResumeVersionCompareResponse {
    @Schema(description = "旧版本ID")
    private Long oldVersionId;
    @Schema(description = "旧版本名称")
    private String oldVersionName;
    @Schema(description = "新版本ID")
    private Long newVersionId;

    @Schema(description = "新版本名称")
    private String newVersionName;

    @Schema(description = "旧版本内容")
    private String oldContent;

    @Schema(description = "新版本内容")
    private String newContent;
    @Schema(description = "新增行列表")
    private List<String> addedLines;
    @Schema(description = "删除行列表")
    private List<String> removedLines;
    @Schema(description = "相同行列表")
    private List<String> commonLines;
    @Schema(description = "旧版本长度")
    private Integer oldLength;
    @Schema(description = "新版本长度")
    private Integer newLength;
    @Schema(description = "新增行数")
    private Integer addedCount;
    @Schema(description = "删除行数")
    private Integer removedCount;
}