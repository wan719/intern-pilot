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

  public static String buildInterviewQuestionPrompt(
      String resumeText,
      String jobDescription,
      String analysisReport) {
    return """
        你是一个资深 Java 后端面试官、技术招聘官和大学生实习求职导师。

        请根据下面的【学生简历文本】、【目标岗位 JD】和【AI 匹配分析报告】，为该学生生成一套适合该岗位的实习面试准备题。

        要求：
        1. 题目要贴合岗位 JD；
        2. 题目要结合学生简历中的项目经历；
        3. 项目追问题要针对简历中的具体项目；
        4. 如果分析报告中提到缺失技能，需要生成对应补强题；
        5. 题目难度要符合大学生暑期实习面试；
        6. 答案要简洁但有要点；
        7. 不要生成过于宽泛的问题；
        8. 必须严格返回 JSON；
        9. 不要返回 Markdown；
        10. 不要使用 ```json 代码块包裹。

        请生成 15 到 20 道题，题目类型包含：
        - JAVA_BASIC
        - SPRING_BOOT
        - SPRING_SECURITY
        - MYSQL
        - REDIS
        - PROJECT
        - HR
        - RESUME
        - JOB_SKILL

        难度只能是：
        - EASY
        - MEDIUM
        - HARD

        返回格式如下：

        {
          "title": "腾讯 Java后端开发实习生 面试题准备",
          "questions": [
            {
              "questionType": "SPRING_SECURITY",
              "difficulty": "MEDIUM",
              "question": "请介绍 Spring Security 的过滤器链执行流程。",
              "answer": "Spring Security 通过一组过滤器对请求进行认证和授权处理...",
              "answerPoints": [
                "请求先经过 SecurityFilterChain",
                "JWT 项目中会经过自定义 JwtAuthenticationFilter",
                "认证成功后将 Authentication 放入 SecurityContext"
              ],
              "relatedSkills": [
                "Spring Security",
                "JWT",
                "Filter"
              ]
            }
          ]
        }

        【学生简历文本】
        %s

        【目标岗位 JD】
        %s

        【AI 匹配分析报告】
        %s
        """.formatted(resumeText, jobDescription, analysisReport);
  }
}
