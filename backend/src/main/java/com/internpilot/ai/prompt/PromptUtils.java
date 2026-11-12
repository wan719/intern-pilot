package com.internpilot.ai.prompt;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "提示词工具类，提供了构建针对AI模型的职位匹配分析、面试问题生成和简历优化等功能的提示词的方法，这些提示词会被用来指导AI模型生成符合要求的输出内")//这个注解用于Swagger API文档生成，提供了对该类的描述信息
public class PromptUtils {

  private PromptUtils() {
  }

  public static String buildResumeJobMatchPrompt(String resumeText, String jobDescription) {
    return """
        你是一个资深技术招聘官、Java 后端面试官和大学生实习求职导师

        请根据下面的【学生简历文本】和【目标岗JD】，分析该学生与岗位的匹配程度

        你的任务
        1. 给出 0-100 的匹配分matchScore
        2. 给出匹配等级 matchLevel
        3. 分析该学生简历中的优strengths
        4. 分析该学生简历中的短weaknesses
        5. 提取岗位要求中该学生可能缺失的技missingSkills
        6. 给出具体的简历优化建suggestions
        7. 给出面试准备建议 interviewTips

        评分要求
        - 如果简历中有明确相关项目经验、技术栈高度匹配，分数可以较高；
        - 如果只是课程经历或泛泛描述，分数应适中
        - 如果缺少岗位核心技能，分数应降低；
        - 不要给过于虚高的分数
        - 建议要具体，不能只写"继续努力"

        你必须使用简体中文回答
        所有字段值必须使用简体中文
        除技术名词外，不要输出英文解释
        必须严格返回 JSON
        不要返回 Markdown
        不要返回多余解释
        不要使用 ```json 代码块包裹

        返回格式如下

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
            "缺失技",
            "缺失技"
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

        匹配等级规则
        - 85-100: HIGH
        - 70-84: MEDIUM_HIGH
        - 60-69: MEDIUM
        - 40-59: LOW
        - 0-39: VERY_LOW

        【学生简历文本
        %s

        【目标岗JD
        %s
        """.formatted(resumeText, jobDescription);
  }

  public static String buildInterviewQuestionPrompt(
      String resumeText,
      String jobDescription,
      String analysisReport) {
    return """
        你是一个资Java 后端面试官、技术招聘官和大学生实习求职导师

        请根据下面的【学生简历文本】、【目标岗JD】和【AI 匹配分析报告】，为该学生生成一套适合该岗位的实习面试准备题

        要求
        1. 题目要贴合岗JD
        2. 题目要结合学生简历中的项目经历；
        3. 项目追问题要针对简历中的具体项目；
        4. 如果分析报告中提到缺失技能，需要生成对应补强题
        5. 题目难度要符合大学生暑期实习面试
        6. 答案要简洁但有要点；
        7. 不要生成过于宽泛的问题；
        8. 你必须使用简体中文回答；
        9. 所有字段值必须使用简体中文；
        10. 除技术名词外，不要输出英文解释；
        11. 必须严格返回 JSON
        12. 不要返回 Markdown
        13. 不要使用 ```json 代码块包裹

        请生15 20 道题，题目类型包含：
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

        返回格式如下

        {
          "title": "腾讯 Java后端开发实习生 面试题准",
          "questions": [
            {
              "questionType": "SPRING_SECURITY",
              "difficulty": "MEDIUM",
              "question": "请介Spring Security 的过滤器链执行流程",
              "answer": "Spring Security 通过一组过滤器对请求进行认证和授权处理...",
              "answerPoints": [
                "请求先经SecurityFilterChain",
                "JWT 项目中会经过自定JwtAuthenticationFilter",
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

        【学生简历文本
        %s

        【目标岗JD
        %s

        【AI 匹配分析报告
        %s
        """.formatted(resumeText, jobDescription, analysisReport);
  }

  public static String buildResumeOptimizePrompt(
      String resumeContent,
      String jobDescription,
      String analysisReport,
      String extraRequirement) {
    return """
        你是一个资Java 后端简历优化导师和技术面试官

        请根据下面的【原始简历内容】、【目标岗JD】和【AI 匹配分析报告】，生成一份更适合该岗位投递的简历优化版本

        要求
        1. 不要编造用户没有的经历
        2. 可以优化表达方式，让项目经历更贴合岗位；
        3. 突出和岗位相关的技术栈
        4. 对缺失技能只能建议补充，不能假装用户已经掌握
        5. 保持大学生实习简历风格；
        6. 输出完整简历文本；
        7. 你必须使用简体中文回答；
        8. 除技术名词外，不要输出英文解释；
        9. 不要返回 Markdown 代码块；
        10. 不要添加解释，只返回优化后的简历内容

        【原始简历内容
        %s

        【目标岗JD
        %s

        【AI 匹配分析报告
        %s

        【用户额外优化要求
        %s
        """.formatted(
        resumeContent,
        jobDescription,
        analysisReport,
        extraRequirement == null ? "" : extraRequirement);
  }

  public static String buildAnalysisPrompt(
      String resumeText,
      String jobDescription,
      String ragContext) {
    return """
        你是一个资Java 后端面试官和实习招聘导师

        请根据【学生简历】、【目标岗位JD】和【岗位知识库参考内容】，
        分析该学生与岗位的匹配度

        要求
        1. 输出 0-100 的匹配分matchScore
        2. 输出匹配等级 matchLevel
        3. 输出简历优strengths
        4. 输出简历短weaknesses
        5. 输出缺失技missingSkills
        6. 输出简历优化建suggestions
        7. 输出面试准备建议 interviewTips
        8. 你必须使用简体中文回答；
        9. 所有字段值必须使用简体中文；
        10. 除技术名词外，不要输出英文解释；
        11. 必须严格返回 JSON
        12. 不要返回 Markdown 代码块；
        13. 不要添加任何 JSON 之外的解释文字

        匹配等级只能使用以下枚举值：
        - HIGH
        - MEDIUM_HIGH
        - MEDIUM
        - LOW
        - VERY_LOW

        必须严格按下面的 JSON 结构返回，字段名不能翻译、不能改名、不能省略：

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
            "缺失技",
            "缺失技"
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

        【岗位知识库参考内容
        %s

        【学生简历
        %s

        【目标岗位JD
        %s
        """.formatted(
        ragContext == null ? "暂无相关知识库内容" : ragContext,
        resumeText,
        jobDescription);
  }
}
