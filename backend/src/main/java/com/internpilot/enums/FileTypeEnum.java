package com.internpilot.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "文件类型枚举，定义系统中支持的文件类型，每个类型对应一个唯一的code和文件扩展名")//这个注解用于Swagger API文档生成，提供了对该枚举类的描述信息
public enum FileTypeEnum {//文件类型枚举，定义系统中支持的文件类型，
// 每个类型对应一个唯一的code和文件扩展名

    PDF("PDF", "pdf"),
    DOCX("DOCX", "docx");

    private final String code;
    private final String extension;

    FileTypeEnum(String code, String extension) {
        this.code = code;
        this.extension = extension;
    }

    /**
     * 根据文件扩展名获取对应的文件类型枚举实例
     *
     * @param extension 文件扩展名
     * @return 对应的文件类型枚举实例，如果未找到则返回null
     */
    public static FileTypeEnum fromExtension(String extension) {
        if (extension == null) {
            return null;
        }

        String lower = extension.toLowerCase();
        for (FileTypeEnum type : values()) {
            if (type.extension.equals(lower)) {
                return type;
            }
        }
        return null;
    }
}
