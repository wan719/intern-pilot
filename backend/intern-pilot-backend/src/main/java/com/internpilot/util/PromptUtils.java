package com.internpilot.util;

public class PromptUtils {

    private PromptUtils() {
    }

    public static String buildResumeJobMatchPrompt(String resumeText, String jobDescription) {
        return """
                你是一个资深技术招聘官、Java 后端面试官和大学生实习求职导师。

                请根据下面的【学生简历文本】和【目标岗位 JD】，分析该学生与岗位的匹配程度。

                你的任务：
                1. 给出 0-100 的匹配分数 matchScore；
                2. 给出匹配等级 matchLevel；
                3. 分析该学生简历中的优势 strengths；
                4. 分析该学生简历中的短板 weaknesses；
                5. 提取岗位要求中该学生可能缺失的技能 missingSkills；
                6. 给出具体的简历优化建议 suggestions；
                7. 给出面试准备建议 interviewTips。

                评分要求：
                - 如果简历中有明确相关项目经验、技术栈高度匹配，分数可以较高；
                - 如果只是课程经历或泛泛描述，分数应适中；
                - 如果缺少岗位核心技能，分数应降低；
                - 不要给过于虚高的分数；
                - 建议要具体，不能只写“继续努力”。

                必须严格返回 JSON。
                不要返回 Markdown。
                不要返回多余解释。
                不要使用 ```json 代码块包裹。

                返回格式如下：

                {
                  "matchScore": 82,
                  "matchLevel": "MEDIUM_HIGH",
                  "strengths": [
                    "优势1",
                    "优势2"
                  ],
                  "weaknesses": [
                    "短板1",
                    "短板2"
                  ],
                  "missingSkills": [
                    "缺失技能1",
                    "缺失技能2"
                  ],
                  "suggestions": [
                    "优化建议1",
                    "优化建议2"
                  ],
                  "interviewTips": [
                    "面试准备建议1",
                    "面试准备建议2"
                  ]
                }

                匹配等级规则：
                - 85-100: HIGH
                - 70-84: MEDIUM_HIGH
                - 60-69: MEDIUM
                - 40-59: LOW
                - 0-39: VERY_LOW

                【学生简历文本】
                %s

                【目标岗位 JD】
                %s
                """.formatted(resumeText, jobDescription);
    }
}
