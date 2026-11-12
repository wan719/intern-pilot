package com.internpilot.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResult<T> {//分页结果类，使用泛型支持不同类型的数据返回
    @Schema(description = "分页数据列表")
    private List<T> records;
    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页码")
    private Long pageNum;

    @Schema(description = "每页记录数")
    private Long pageSize;

    @Schema(description = "总页数")
    private Long pages;
}