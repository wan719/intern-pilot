package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum FileTypeEnum {

    PDF("PDF", "pdf"),
    DOCX("DOCX", "docx");

    private final String code;
    private final String extension;

    FileTypeEnum(String code, String extension) {
        this.code = code;
        this.extension = extension;
    }

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
