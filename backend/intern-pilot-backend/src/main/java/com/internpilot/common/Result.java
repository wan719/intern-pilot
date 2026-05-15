package com.internpilot.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "通用响应结果类，包含状态码、消息和数据")
@Data // 使用Lombok自动生成getter、setter、toString等方法
@NoArgsConstructor // 使用Lombok生成无参构造方法
@AllArgsConstructor // 使用Lombok生成全参构造方法
@Builder // 使用Lombok构建器模式创建对象
public class Result<T> {
    private Integer code; // 响应状态码
    private String message; // 响应消息
    private T data; // 响应数据，使用泛型支持不同类型

   @Schema(description = "成功响应（无数据）")
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    @Schema(description = "成功响应（包含数据）")
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    @Schema(description = "失败响应（自定义错误码和消息）")
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    @Schema(description = "失败响应（使用ResultCode枚举）")
    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    @Schema(description = "失败响应（使用ResultCode枚举和自定义消息）")
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }

}
