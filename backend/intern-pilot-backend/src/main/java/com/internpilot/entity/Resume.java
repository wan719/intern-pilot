package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("resume")
public class Resume {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String resumeName;

    private String originalFileName;

    private String storedFileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    private String parsedText;

    private String parseStatus;

    private Integer isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
