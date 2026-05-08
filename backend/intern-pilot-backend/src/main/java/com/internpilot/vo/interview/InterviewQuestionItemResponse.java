package com.internpilot.vo.interview;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "面试题详情项")
public class InterviewQuestionItemResponse {

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "题目类型")
    private String questionType;

    @Schema(description = "难度")
    private String difficulty;

    @Schema(description = "题目")
    private String question;

    @Schema(description = "参考答案")
    private String answer;

    @Schema(description = "答题要点")
    private List<String> answerPoints;

    @Schema(description = "相关技能")
    private List<String> relatedSkills;

    @Schema(description = "排序")
    private Integer sortOrder;
}