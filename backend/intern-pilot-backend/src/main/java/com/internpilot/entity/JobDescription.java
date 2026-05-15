package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_description")//职位描述表，用户可以保存自己感兴趣的职位描述
@Schema(description = "职位描述实体类，包含职位描述的详细信息")
public class JobDescription {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String companyName;

    private String jobTitle;

    private String jobType;

    private String location;

    private String sourcePlatform;

    private String jobUrl;

    private String jdContent;

    private String skillRequirements;

    private String salaryRange;

    private String workDaysPerWeek;

    private String internshipDuration;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
