# InternPilot 简历上传与解析模块设计文档

## 1. 文档说明

本文档用于描述 InternPilot 项目的简历上传与解析模块设计，包括模块目标、业务流程、数据库设计、接口设计、文件存储设计、文件校验设计、PDF/DOCX 解析方案、Service 设计、异常处理、安全设计、测试流程和后续扩展方案。

简历上传与解析模块是 InternPilot 的核心输入模块。用户上传简历后，系统需要保存简历文件，并将 PDF 或 DOCX 中的文本提取出来，作为后续 AI 匹配分析的输入。

---

## 2. 模块目标

简历上传与解析模块需要完成以下目标：

1. 支持用户上传简历文件；
2. 支持 PDF 格式简历；
3. 支持 DOCX 格式简历；
4. 校验文件是否为空；
5. 校验文件大小；
6. 校验文件类型；
7. 将文件保存到本地目录；
8. 生成安全的存储文件名；
9. 解析简历文本；
10. 将简历元数据和解析文本保存到数据库；
11. 支持查询简历列表；
12. 支持查询简历详情；
13. 支持删除简历；
14. 支持设置默认简历；
15. 保证用户只能访问自己的简历。

---

## 3. 模块业务定位

在 InternPilot 中，简历模块承担的是“用户能力信息输入”的角色。

整体业务链路如下：

```text
用户上传简历
  ↓
系统保存文件
  ↓
系统解析简历文本
  ↓
保存 parsedText
  ↓
用户创建岗位 JD
  ↓
AI 分析模块读取 parsedText + jdContent
  ↓
生成岗位匹配报告
```

所以，简历模块的质量会直接影响 AI 分析模块的效果。

## 4. 功能范围

### 4.1 第一阶段必须实现

第一阶段需要实现：

1. 上传 PDF 简历；
2. 上传 DOCX 简历；
3. 文件大小限制；
4. 文件类型限制；
5. 本地文件存储；
6. PDF 文本解析；
7. DOCX 文本解析；
8. 简历列表查询；
9. 简历详情查询；
10. 删除简历；
11. 设置默认简历；
12. 数据权限控制。

### 4.2 第二阶段可扩展

第二阶段可以扩展：

1. 支持 TXT；
2. 支持 Markdown；
3. 支持用户手动编辑 `parsedText`；
4. 支持 MinIO 对象存储；
5. 支持 OCR 解析扫描版 PDF；
6. 支持简历版本对比；
7. 支持简历内容结构化提取；
8. 支持 AI 自动优化简历版本。

## 5. 数据库设计

### 5.1 `resume` 表

简历模块主要使用 `resume` 表。

```sql
CREATE TABLE IF NOT EXISTS resume (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '简历ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    resume_name VARCHAR(100) DEFAULT NULL COMMENT '简历名称',
    original_file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型：PDF/DOCX',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小，单位字节',
    parsed_text LONGTEXT DEFAULT NULL COMMENT '解析后的文本',
    parse_status VARCHAR(30) NOT NULL DEFAULT 'SUCCESS' COMMENT '解析状态：SUCCESS/FAILED/PENDING',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认简历：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：0未删除，1已删除',
    KEY idx_resume_user_id (user_id),
    KEY idx_resume_user_default (user_id, is_default),
    KEY idx_resume_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历表';
```

### 5.2 字段说明

| 字段 | 说明 |
|---|---|
| `id` | 简历 ID |
| `user_id` | 当前简历所属用户 |
| `resume_name` | 用户自定义简历名称 |
| `original_file_name` | 用户上传时的原始文件名 |
| `stored_file_name` | 系统生成的存储文件名 |
| `file_path` | 文件在服务器上的存储路径 |
| `file_type` | 文件类型，PDF 或 DOCX |
| `file_size` | 文件大小，单位字节 |
| `parsed_text` | 从简历中解析出的纯文本 |
| `parse_status` | 解析状态 |
| `is_default` | 是否默认简历 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |
| `deleted` | 逻辑删除标记 |

### 5.3 `parse_status` 枚举

| 状态 | 说明 |
|---|---|
| `SUCCESS` | 解析成功 |
| `FAILED` | 解析失败 |
| `PENDING` | 等待解析 |

第一阶段同步解析，所以主要会用到：

- `SUCCESS`
- `FAILED`

后续如果改成异步解析，可以使用：

- `PENDING`

### 5.4 `file_type` 枚举

| 类型 | 说明 |
|---|---|
| `PDF` | PDF 简历 |
| `DOCX` | Word 简历 |

## 6. 接口设计

### 6.1 上传简历

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/resumes/upload` |
| Method | `POST` |
| 权限 | `USER` |
| Content-Type | `multipart/form-data` |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | `MultipartFile` | 是 | 简历文件 |
| `resumeName` | `String` | 否 | 简历名称 |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "resumeId": 1,
    "resumeName": "Java后端实习简历",
    "originalFileName": "resume.pdf",
    "fileType": "PDF",
    "fileSize": 204800,
    "parseStatus": "SUCCESS",
    "parsedTextPreview": "黎宏 西南大学 软件工程 Java Spring Boot...",
    "isDefault": false,
    "createdAt": "2026-05-06 20:10:00"
  }
}
```

**业务规则**

1. 用户必须登录；
2. 文件不能为空；
3. 文件大小不能超过 10MB；
4. 只允许上传 PDF 和 DOCX；
5. 文件保存到服务器本地目录；
6. 文件名由系统重新生成；
7. 文件保存成功后解析文本；
8. 解析成功后保存 `parsed_text`；
9. 如果用户没有任何简历，第一份简历可以自动设为默认简历。

### 6.2 查询简历列表

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/resumes` |
| Method | `GET` |
| 权限 | `USER` |

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `pageNum` | `Integer` | 否 | `1` | 页码 |
| `pageSize` | `Integer` | 否 | `10` | 每页数量 |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "resumeId": 1,
        "resumeName": "Java后端实习简历",
        "originalFileName": "resume.pdf",
        "fileType": "PDF",
        "fileSize": 204800,
        "parseStatus": "SUCCESS",
        "isDefault": true,
        "createdAt": "2026-05-06 20:10:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

**业务规则**

1. 只能查询当前用户的简历；
2. 默认按创建时间倒序；
3. 列表接口不返回完整 `parsedText`；
4. 列表接口可以返回文本预览，但不建议返回全文。

### 6.3 查询简历详情

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/resumes/{id}` |
| Method | `GET` |
| 权限 | `USER` |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "resumeId": 1,
    "resumeName": "Java后端实习简历",
    "originalFileName": "resume.pdf",
    "storedFileName": "1_20260506_9f8a_resume.pdf",
    "filePath": "uploads/resumes/user-1/1_20260506_9f8a_resume.pdf",
    "fileType": "PDF",
    "fileSize": 204800,
    "parseStatus": "SUCCESS",
    "isDefault": true,
    "parsedText": "黎宏，西南大学软件工程专业，项目经历包括...",
    "createdAt": "2026-05-06 20:10:00",
    "updatedAt": "2026-05-06 20:10:00"
  }
}
```

**业务规则**

1. 简历必须存在；
2. 简历必须属于当前用户；
3. 返回完整解析文本；
4. 如果简历不存在，返回 `404`；
5. 如果访问他人简历，返回 `403` 或 `404`。

### 6.4 删除简历

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/resumes/{id}` |
| Method | `DELETE` |
| 权限 | `USER` |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**业务规则**

1. 只能删除自己的简历；
2. 第一阶段采用逻辑删除；
3. 删除后列表不再显示；
4. 如果删除的是默认简历，则需要取消默认状态；
5. 历史 AI 分析报告暂时保留。

### 6.5 设置默认简历

**基本信息**

| 项目 | 内容 |
|---|---|
| URL | `/api/resumes/{id}/default` |
| Method | `PUT` |
| 权限 | `USER` |

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

**业务规则**

1. 只能设置自己的简历；
2. 每个用户最多只能有一份默认简历；
3. 设置新的默认简历时，需要取消原来的默认简历；
4. 被逻辑删除的简历不能设置为默认简历。

## 7. 文件存储设计

### 7.1 第一阶段存储方案

第一阶段使用本地文件存储。

配置项：

```yaml
file:
  upload-dir: uploads/resumes
  max-size: 10485760
  allowed-types:
    - pdf
    - docx
```

### 7.2 目录结构

建议按用户分目录存储：

```text
uploads/
  resumes/
    user-1/
      1_20260506_9f8a_resume.pdf
      1_20260506_2abc_resume.docx
    user-2/
      2_20260506_3def_resume.pdf
```

这样做的好处：

1. 不同用户文件隔离；
2. 文件路径清晰；
3. 后续迁移到 MinIO 时容易映射；
4. 方便排查问题。

### 7.3 存储文件名生成规则

不要直接使用用户上传的文件名作为存储文件名。

建议规则：

```text
{userId}_{yyyyMMddHHmmss}_{random}.{ext}
```

示例：

```text
1_20260506201030_a8f3c2.pdf
```

原因：

1. 避免文件重名；
2. 避免中文文件名导致路径问题；
3. 避免用户构造危险文件名；
4. 便于追踪文件所属用户和上传时间。

### 7.4 原始文件名保存

虽然实际存储文件名由系统生成，但数据库仍然保存用户上传时的原始文件名：`original_file_name`。

这样前端展示时更友好。

## 8. 文件校验设计

### 8.1 校验内容

上传文件时需要进行以下校验：

1. 文件是否为空；
2. 文件大小是否超过限制；
3. 文件扩展名是否合法；
4. 文件 MIME 类型是否合法；
5. 文件名是否安全；
6. 解析文本是否为空。

### 8.2 文件大小限制

第一阶段限制：

```text
最大 10MB
```

### 8.3 文件扩展名校验

允许：

- `.pdf`
- `.docx`

不允许：

- `.exe`
- `.bat`
- `.sh`
- `.js`
- `.html`
- `.zip`
- `.rar`
- `.doc`

第一阶段不支持老版 `.doc`，因为解析复杂度更高。

### 8.4 MIME 类型校验

建议同时校验 MIME 类型。

| 文件 | 常见 MIME |
|---|---|
| PDF | `application/pdf` |
| DOCX | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` |

注意：MIME 类型可能被浏览器或客户端错误设置，所以不能只依赖 MIME，也要结合扩展名判断。

### 8.5 文件名安全处理

用户上传的原始文件名不能直接用于存储路径。

需要避免：

- `../../evil.pdf`
- `C:\Windows\system32\xxx`
- `<script>.pdf`

处理方式：

1. 原始文件名只用于展示；
2. 存储文件名由后端生成；
3. 文件保存路径由后端配置决定；
4. 不允许用户传入保存路径。

## 9. PDF 解析设计

### 9.1 技术选型

使用：`Apache PDFBox`

Gradle 依赖：

```gradle
implementation 'org.apache.pdfbox:pdfbox:2.0.31'
```

### 9.2 解析流程

```text
读取 MultipartFile
  ↓
转换为 InputStream
  ↓
使用 PDDocument.load(inputStream)
  ↓
使用 PDFTextStripper 提取文本
  ↓
清洗文本
  ↓
返回 parsedText
```

### 9.3 PDF 解析示例代码

```java
private String parsePdf(MultipartFile file) {
    try (PDDocument document = PDDocument.load(file.getInputStream())) {
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        return cleanText(text);
    } catch (IOException e) {
        throw new FileParseException("PDF 简历解析失败");
    }
}
```

### 9.4 PDF 解析限制

PDFBox 对以下情况支持有限：

1. 扫描版 PDF；
2. 图片型 PDF；
3. 复杂双栏排版；
4. 加密 PDF；
5. 特殊字体 PDF。

第一阶段只支持文本型 PDF。后续如果支持扫描版 PDF，需要接入 OCR。

## 10. DOCX 解析设计

### 10.1 技术选型

使用：`Apache POI`

Gradle 依赖：

```gradle
implementation 'org.apache.poi:poi-ooxml:5.2.5'
```

### 10.2 解析流程

```text
读取 MultipartFile
  ↓
转换为 InputStream
  ↓
使用 XWPFDocument 读取 DOCX
  ↓
遍历段落
  ↓
提取文本
  ↓
清洗文本
  ↓
返回 parsedText
```

### 10.3 DOCX 解析示例代码

```java
private String parseDocx(MultipartFile file) {
    try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
        StringBuilder builder = new StringBuilder();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText();
            if (text != null && !text.isBlank()) {
                builder.append(text).append("\n");
            }
        }

        return cleanText(builder.toString());
    } catch (IOException e) {
        throw new FileParseException("DOCX 简历解析失败");
    }
}
```

### 10.4 DOCX 解析限制

Apache POI 对普通段落支持较好，但对以下内容可能解析不完整：

1. 文本框；
2. 页眉页脚；
3. 表格中的复杂内容；
4. 图片中的文字；
5. 特殊样式布局。

后续可以增强表格解析。

## 11. 文本清洗设计

### 11.1 为什么需要文本清洗

从 PDF 或 DOCX 中提取出的文本可能存在：

1. 多余空行；
2. 多个连续空格；
3. 换行混乱；
4. 特殊不可见字符；
5. 乱码；
6. 文本过长。

这些会影响 AI 分析质量。

### 11.2 清洗规则

第一阶段可以采用简单清洗：

1. 去除首尾空格；
2. 将 Windows 换行统一为 `\n`；
3. 多个空行压缩为一个空行；
4. 多个空格压缩为一个空格；
5. 限制最大文本长度；
6. 如果清洗后文本为空，标记解析失败。

### 11.3 示例代码

```java
private String cleanText(String text) {
    if (text == null) {
        return "";
    }

    String cleaned = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("[ \\t]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();

    int maxLength = 20000;
    if (cleaned.length() > maxLength) {
        cleaned = cleaned.substring(0, maxLength);
    }

    return cleaned;
}
```

## 12. 枚举设计

### 12.1 `FileTypeEnum`

路径：

```text
src/main/java/com/internpilot/enums/FileTypeEnum.java
```

```java
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
```

### 12.2 `ParseStatusEnum`

路径：

```text
src/main/java/com/internpilot/enums/ParseStatusEnum.java
```

```java
package com.internpilot.enums;

import lombok.Getter;

@Getter
public enum ParseStatusEnum {

    SUCCESS("SUCCESS", "解析成功"),
    FAILED("FAILED", "解析失败"),
    PENDING("PENDING", "等待解析");

    private final String code;
    private final String description;

    ParseStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
```

## 13. Entity 设计

### 13.1 `Resume` Entity

路径：

```text
src/main/java/com/internpilot/entity/Resume.java
```

```java
package com.internpilot.entity;

import com.baomidou.mybatisplus.annotation.*;
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
```

### 13.2 `ResumeMapper`

路径：

```text
src/main/java/com/internpilot/mapper/ResumeMapper.java
```

```java
package com.internpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.entity.Resume;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeMapper extends BaseMapper<Resume> {
}
```

## 14. DTO 与 VO 设计

### 14.1 `ResumeUploadResponse`

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeUploadResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历上传响应")
public class ResumeUploadResponse {

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "解析文本预览")
    private String parsedTextPreview;

    @Schema(description = "是否默认简历")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 14.2 `ResumeListResponse`

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeListResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历列表响应")
public class ResumeListResponse {

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "是否默认简历")
    private Boolean isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
```

### 14.3 `ResumeDetailResponse`

路径：

```text
src/main/java/com/internpilot/vo/resume/ResumeDetailResponse.java
```

```java
package com.internpilot.vo.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "简历详情响应")
public class ResumeDetailResponse {

    @Schema(description = "简历ID")
    private Long resumeId;

    @Schema(description = "简历名称")
    private String resumeName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "存储文件名")
    private String storedFileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "是否默认简历")
    private Boolean isDefault;

    @Schema(description = "解析后的完整文本")
    private String parsedText;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

## 15. 配置类设计

### 15.1 `FileStorageProperties`

路径：

```text
src/main/java/com/internpilot/config/FileStorageProperties.java
```

```java
package com.internpilot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "file")
public class FileStorageProperties {

    private String uploadDir;

    private Long maxSize;

    private List<String> allowedTypes;
}
```

## 16. Service 设计

### 16.1 `FileStorageService`

职责：

1. 保存文件；
2. 创建目录；
3. 生成存储文件名；
4. 返回文件路径。

路径：

```text
src/main/java/com/internpilot/service/FileStorageService.java
```

```java
package com.internpilot.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    StoredFileInfo store(MultipartFile file, Long userId);
}
```

### 16.2 `StoredFileInfo`

路径：

```text
src/main/java/com/internpilot/service/StoredFileInfo.java
```

```java
package com.internpilot.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoredFileInfo {

    private String originalFileName;

    private String storedFileName;

    private String filePath;

    private String fileType;

    private Long fileSize;
}
```

### 16.3 `LocalFileStorageServiceImpl`

路径：

```text
src/main/java/com/internpilot/service/impl/LocalFileStorageServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.internpilot.config.FileStorageProperties;
import com.internpilot.enums.FileTypeEnum;
import com.internpilot.exception.FileParseException;
import com.internpilot.service.FileStorageService;
import com.internpilot.service.StoredFileInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public StoredFileInfo store(MultipartFile file, Long userId) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        FileTypeEnum fileType = FileTypeEnum.fromExtension(extension);

        if (fileType == null) {
            throw new FileParseException("仅支持 PDF 或 DOCX 文件");
        }

        String storedFileName = generateStoredFileName(userId, extension);

        Path userDir = Paths.get(
                fileStorageProperties.getUploadDir(),
                "user-" + userId
        );

        try {
            Files.createDirectories(userDir);

            Path targetPath = userDir.resolve(storedFileName).normalize();

            file.transferTo(targetPath.toFile());

            return new StoredFileInfo(
                    originalFileName,
                    storedFileName,
                    targetPath.toString(),
                    fileType.getCode(),
                    file.getSize()
            );
        } catch (IOException e) {
            throw new FileParseException("简历文件保存失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileParseException("上传文件不能为空");
        }

        Long maxSize = fileStorageProperties.getMaxSize();
        if (maxSize != null && file.getSize() > maxSize) {
            throw new FileParseException("文件大小不能超过 10MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new FileParseException("文件名不能为空");
        }

        String extension = getExtension(originalFileName);
        if (FileTypeEnum.fromExtension(extension) == null) {
            throw new FileParseException("仅支持 PDF 或 DOCX 文件");
        }
    }

    private String generateStoredFileName(Long userId, String extension) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return userId + "_" + time + "_" + random + "." + extension;
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase();
    }
}
```

### 16.4 `ResumeParseService`

职责：

1. 根据文件类型调用不同解析器；
2. 解析 PDF；
3. 解析 DOCX；
4. 清洗文本；
5. 返回解析结果。

路径：

```text
src/main/java/com/internpilot/service/ResumeParseService.java
```

```java
package com.internpilot.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumeParseService {

    String parse(MultipartFile file, String fileType);
}
```

### 16.5 `ResumeParseServiceImpl`

路径：

```text
src/main/java/com/internpilot/service/impl/ResumeParseServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.internpilot.enums.FileTypeEnum;
import com.internpilot.exception.FileParseException;
import com.internpilot.service.ResumeParseService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeParseServiceImpl implements ResumeParseService {

    private static final int MAX_TEXT_LENGTH = 20000;

    @Override
    public String parse(MultipartFile file, String fileType) {
        String text;

        if (FileTypeEnum.PDF.getCode().equals(fileType)) {
            text = parsePdf(file);
        } else if (FileTypeEnum.DOCX.getCode().equals(fileType)) {
            text = parseDocx(file);
        } else {
            throw new FileParseException("不支持的简历文件类型");
        }

        String cleaned = cleanText(text);

        if (cleaned.isBlank()) {
            throw new FileParseException("简历解析文本为空，请检查文件内容");
        }

        return cleaned;
    }

    private String parsePdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new FileParseException("PDF 简历解析失败");
        }
    }

    private String parseDocx(MultipartFile file) {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder builder = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    builder.append(text).append("\n");
                }
            }

            return builder.toString();
        } catch (IOException e) {
            throw new FileParseException("DOCX 简历解析失败");
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String cleaned = text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (cleaned.length() > MAX_TEXT_LENGTH) {
            cleaned = cleaned.substring(0, MAX_TEXT_LENGTH);
        }

        return cleaned;
    }
}
```

## 17. `ResumeService` 设计

### 17.1 `ResumeService`

路径：

```text
src/main/java/com/internpilot/service/ResumeService.java
```

```java
package com.internpilot.service;

import com.internpilot.common.PageResult;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ResumeService {

    ResumeUploadResponse upload(MultipartFile file, String resumeName);

    PageResult<ResumeListResponse> list(Integer pageNum, Integer pageSize);

    ResumeDetailResponse getDetail(Long id);

    Boolean delete(Long id);

    Boolean setDefault(Long id);
}
```

### 17.2 `ResumeServiceImpl` 核心逻辑

路径：

```text
src/main/java/com/internpilot/service/impl/ResumeServiceImpl.java
```

```java
package com.internpilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.internpilot.common.PageResult;
import com.internpilot.entity.Resume;
import com.internpilot.enums.ParseStatusEnum;
import com.internpilot.exception.BusinessException;
import com.internpilot.mapper.ResumeMapper;
import com.internpilot.service.FileStorageService;
import com.internpilot.service.ResumeParseService;
import com.internpilot.service.ResumeService;
import com.internpilot.service.StoredFileInfo;
import com.internpilot.util.SecurityUtils;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeMapper resumeMapper;
    private final FileStorageService fileStorageService;
    private final ResumeParseService resumeParseService;

    @Override
    @Transactional
    public ResumeUploadResponse upload(MultipartFile file, String resumeName) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        StoredFileInfo fileInfo = fileStorageService.store(file, currentUserId);
        String parsedText = resumeParseService.parse(file, fileInfo.getFileType());

        Long resumeCount = resumeMapper.selectCount(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
        );

        boolean isFirstResume = resumeCount == null || resumeCount == 0;

        Resume resume = new Resume();
        resume.setUserId(currentUserId);
        resume.setResumeName(resumeName);
        resume.setOriginalFileName(fileInfo.getOriginalFileName());
        resume.setStoredFileName(fileInfo.getStoredFileName());
        resume.setFilePath(fileInfo.getFilePath());
        resume.setFileType(fileInfo.getFileType());
        resume.setFileSize(fileInfo.getFileSize());
        resume.setParsedText(parsedText);
        resume.setParseStatus(ParseStatusEnum.SUCCESS.getCode());
        resume.setIsDefault(isFirstResume ? 1 : 0);

        resumeMapper.insert(resume);

        return toUploadResponse(resume);
    }

    @Override
    public PageResult<ResumeListResponse> list(Integer pageNum, Integer pageSize) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<Resume> page = new Page<>(pageNum, pageSize);

        Page<Resume> resultPage = resumeMapper.selectPage(
                page,
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
                        .orderByDesc(Resume::getCreatedAt)
        );

        List<ResumeListResponse> records = resultPage.getRecords()
                .stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public ResumeDetailResponse getDetail(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);
        return toDetailResponse(resume);
    }

    @Override
    @Transactional
    public Boolean delete(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);

        resume.setDeleted(1);
        resume.setIsDefault(0);
        resumeMapper.updateById(resume);

        return true;
    }

    @Override
    @Transactional
    public Boolean setDefault(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Resume resume = getUserResumeOrThrow(id, currentUserId);

        Resume updateOldDefault = new Resume();
        updateOldDefault.setIsDefault(0);

        resumeMapper.update(
                updateOldDefault,
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getUserId, currentUserId)
                        .eq(Resume::getDeleted, 0)
        );

        Resume updateNewDefault = new Resume();
        updateNewDefault.setId(resume.getId());
        updateNewDefault.setIsDefault(1);
        resumeMapper.updateById(updateNewDefault);

        return true;
    }

    private Resume getUserResumeOrThrow(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectOne(
                new LambdaQueryWrapper<Resume>()
                        .eq(Resume::getId, resumeId)
                        .eq(Resume::getUserId, userId)
                        .eq(Resume::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (resume == null) {
            throw new BusinessException("简历不存在或无权限访问");
        }

        return resume;
    }

    private ResumeUploadResponse toUploadResponse(Resume resume) {
        ResumeUploadResponse response = new ResumeUploadResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setParsedTextPreview(buildPreview(resume.getParsedText()));
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setCreatedAt(resume.getCreatedAt());
        return response;
    }

    private ResumeListResponse toListResponse(Resume resume) {
        ResumeListResponse response = new ResumeListResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setCreatedAt(resume.getCreatedAt());
        return response;
    }

    private ResumeDetailResponse toDetailResponse(Resume resume) {
        ResumeDetailResponse response = new ResumeDetailResponse();
        response.setResumeId(resume.getId());
        response.setResumeName(resume.getResumeName());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setStoredFileName(resume.getStoredFileName());
        response.setFilePath(resume.getFilePath());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setParseStatus(resume.getParseStatus());
        response.setIsDefault(resume.getIsDefault() != null && resume.getIsDefault() == 1);
        response.setParsedText(resume.getParsedText());
        response.setCreatedAt(resume.getCreatedAt());
        response.setUpdatedAt(resume.getUpdatedAt());
        return response;
    }

    private String buildPreview(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        int maxLength = 100;
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
```

## 18. Controller 设计

### 18.1 `ResumeController`

路径：

```text
src/main/java/com/internpilot/controller/ResumeController.java
```

```java
package com.internpilot.controller;

import com.internpilot.common.PageResult;
import com.internpilot.common.Result;
import com.internpilot.service.ResumeService;
import com.internpilot.vo.resume.ResumeDetailResponse;
import com.internpilot.vo.resume.ResumeListResponse;
import com.internpilot.vo.resume.ResumeUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "简历管理接口")
@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "上传简历", description = "上传 PDF 或 DOCX 简历，并解析文本内容")
    @PostMapping("/upload")
    public Result<ResumeUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "resumeName", required = false) String resumeName
    ) {
        return Result.success(resumeService.upload(file, resumeName));
    }

    @Operation(summary = "查询简历列表", description = "分页查询当前用户上传的简历列表")
    @GetMapping
    public Result<PageResult<ResumeListResponse>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(resumeService.list(pageNum, pageSize));
    }

    @Operation(summary = "查询简历详情", description = "查询当前用户某份简历的详细信息和解析文本")
    @GetMapping("/{id}")
    public Result<ResumeDetailResponse> getDetail(@PathVariable Long id) {
        return Result.success(resumeService.getDetail(id));
    }

    @Operation(summary = "删除简历", description = "逻辑删除当前用户的简历")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(resumeService.delete(id));
    }

    @Operation(summary = "设置默认简历", description = "将当前用户某份简历设置为默认简历")
    @PutMapping("/{id}/default")
    public Result<Boolean> setDefault(@PathVariable Long id) {
        return Result.success(resumeService.setDefault(id));
    }
}
```

## 19. 事务设计

### 19.1 需要事务的方法

以下方法建议加事务：

```java
@Transactional
public ResumeUploadResponse upload(...)
```

原因：

1. 文件保存；
2. 文本解析；
3. 数据库插入；
4. 如果数据库插入失败，需要整体回滚。

注意：本地文件系统不受数据库事务管理。如果数据库保存失败，已经保存的文件不会自动删除。

后续可以优化为：

1. 数据库失败后手动删除文件；
2. 先保存数据库 `PENDING` 记录；
3. 异步解析；
4. 使用对象存储事务补偿策略。

### 19.2 设置默认简历需要事务

```java
@Transactional
public Boolean setDefault(Long id)
```

原因：

1. 先取消旧默认简历；
2. 再设置新默认简历；
3. 两个操作应该保持一致。

## 20. 异常处理设计

### 20.1 `FileParseException`

用于文件上传、保存和解析异常。

```java
public class FileParseException extends BusinessException {

    public FileParseException(String message) {
        super(ResultCode.FILE_PROCESS_ERROR, message);
    }
}
```

### 20.2 常见异常

| 场景 | 异常信息 |
|---|---|
| 文件为空 | 上传文件不能为空 |
| 文件太大 | 文件大小不能超过 10MB |
| 文件类型不支持 | 仅支持 PDF 或 DOCX 文件 |
| 文件保存失败 | 简历文件保存失败 |
| PDF 解析失败 | PDF 简历解析失败 |
| DOCX 解析失败 | DOCX 简历解析失败 |
| 解析文本为空 | 简历解析文本为空，请检查文件内容 |
| 简历不存在 | 简历不存在或无权限访问 |

## 21. 安全设计

### 21.1 数据权限安全

所有简历查询、删除、设置默认操作都必须带：

```text
user_id = 当前登录用户ID
```

不能只根据：

```text
resume_id
```

查询。

错误做法：

```java
resumeMapper.selectById(resumeId);
```

正确做法：

```java
resumeMapper.selectOne(
    new LambdaQueryWrapper<Resume>()
        .eq(Resume::getId, resumeId)
        .eq(Resume::getUserId, currentUserId)
        .eq(Resume::getDeleted, 0)
);
```

### 21.2 文件路径安全

必须保证：

1. 文件目录由后端配置；
2. 用户不能控制文件保存路径；
3. 存储文件名由后端生成；
4. 不使用用户原始文件名作为保存文件名；
5. 使用 `normalize()` 规范路径；
6. 不允许路径穿越。

### 21.3 文件类型安全

第一阶段至少做扩展名校验。

更好的做法：

1. 扩展名校验；
2. MIME 类型校验；
3. 文件头魔数校验。

第一阶段可以先做前两项，后续再增强魔数校验。

### 21.4 隐私安全

简历属于敏感信息，注意：

1. 日志中不要打印完整简历文本；
2. 不要把 `parsedText` 打印到控制台；
3. 不要把上传文件提交到 GitHub；
4. `uploads/` 目录必须加入 `.gitignore`；
5. 用户只能访问自己的简历。

## 22. `.gitignore` 配置

项目根目录或 `backend` 目录中需要加入：

```text
uploads/
*.pdf
*.docx
```

至少需要：

```text
uploads/
```

原因：

1. 用户上传文件不能提交到 GitHub；
2. 简历包含个人隐私；
3. 防止仓库体积变大。

## 23. 测试流程

### 23.1 测试前置条件

1. 项目启动成功；
2. MySQL 启动成功；
3. Redis 启动成功；
4. 用户认证模块完成；
5. 已经注册并登录；
6. 已获得 JWT Token；
7. `resume` 表已创建；
8. `uploads/` 目录可写。

### 23.2 测试顺序

1. 登录获取 Token
2. 上传 PDF 简历
3. 查询简历列表
4. 查询简历详情
5. 上传 DOCX 简历
6. 设置默认简历
7. 查询简历列表验证默认状态
8. 删除简历
9. 再次查询列表验证删除结果
10. 不带 Token 上传简历，验证 401
11. 上传不支持格式，验证 700
12. 上传超大文件，验证 700 或 400

## 24. PowerShell 测试示例

### 24.1 登录获取 Token

```powershell
$body = @{
  username = "wan"
  password = "123456"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/auth/login" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

$token = $response.data.token
```

### 24.2 上传简历

```powershell
$form = @{
  file = Get-Item "D:\resume.pdf"
  resumeName = "Java后端实习简历"
}

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes/upload" `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -Form $form
```

如果你的 PowerShell 版本不支持 `-Form`，可以使用 `curl.exe`。

### 24.3 `curl.exe` 上传简历

```powershell
curl.exe -X POST "http://localhost:8080/api/resumes/upload" `
  -H "Authorization: Bearer YOUR_TOKEN" `
  -F "file=@D:\resume.pdf" `
  -F "resumeName=Java后端实习简历"
```

### 24.4 查询简历列表

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes?pageNum=1&pageSize=10" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 24.5 查询简历详情

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes/1" `
  -Method Get `
  -Headers @{ Authorization = "Bearer $token" }
```

### 24.6 设置默认简历

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes/1/default" `
  -Method Put `
  -Headers @{ Authorization = "Bearer $token" }
```

### 24.7 删除简历

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/resumes/1" `
  -Method Delete `
  -Headers @{ Authorization = "Bearer $token" }
```

## 25. Swagger 测试流程

访问：

```text
http://localhost:8080/doc.html
```

测试步骤：

1. 调用登录接口；
2. 复制返回 Token；
3. 点击 Authorize；
4. 输入 Bearer Token；
5. 打开简历管理接口；
6. 选择上传接口；
7. 选择 PDF 或 DOCX 文件；
8. 输入 `resumeName`；
9. 点击发送；
10. 查看响应结果；
11. 调用列表接口；
12. 调用详情接口。

## 26. 常见问题与解决方案

### 26.1 上传接口返回 401

原因：

1. 没有携带 Token；
2. Token 格式错误；
3. Token 已过期。

正确格式：

```text
Authorization: Bearer your_token
```

### 26.2 上传接口返回 413

原因：文件超过 Spring Boot 上传限制。

解决：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

### 26.3 PDF 解析出来是空

可能原因：

1. PDF 是扫描版；
2. PDF 里面本质是图片；
3. PDF 加密；
4. PDF 字体特殊。

解决：第一阶段提示用户上传文本型 PDF 或 DOCX，后续再接 OCR。

### 26.4 DOCX 表格内容解析不完整

原因：当前示例只解析了段落，没有解析表格。

解决：后续增强表格遍历逻辑，解析 `document.getTables()`。

### 26.5 文件保存成功但数据库失败

原因：本地文件系统不受数据库事务控制。

解决方案：

1. 捕获数据库异常后删除已保存文件；
2. 后续设计文件清理任务；
3. 或者先入库 `PENDING`，再保存文件，再更新状态。

第一阶段可以先接受这个小问题，但文档中需要明确它的存在。

## 27. 后续增强：DOCX 表格解析

很多简历模板会使用表格布局，所以后续可以增强 DOCX 表格解析。

示例逻辑：

```java
document.getTables().forEach(table -> {
    table.getRows().forEach(row -> {
        row.getTableCells().forEach(cell -> {
            builder.append(cell.getText()).append(" ");
        });
        builder.append("\n");
    });
});
```

完整思路：

```text
先解析段落
  ↓
再解析表格
  ↓
合并文本
  ↓
清洗文本
```

## 28. 后续增强：手动编辑解析文本

由于 PDF/DOCX 解析可能不完美，后续可以增加接口：

```text
PUT /api/resumes/{id}/parsed-text
```

用户可以手动修改解析后的文本。

请求示例：

```json
{
  "parsedText": "用户修正后的简历文本..."
}
```

这个功能对 AI 分析质量很重要，因为用户可以把解析错误的内容手动修正。

## 29. 后续增强：MinIO 对象存储

第一阶段使用本地存储，后续可以升级为 MinIO。

架构变化：

```text
用户上传文件
  ↓
后端接收 MultipartFile
  ↓
上传到 MinIO
  ↓
数据库保存 objectName 和 fileUrl
  ↓
解析文件文本
```

## 30. 后续增强：OCR 解析扫描版 PDF

如果用户上传扫描版 PDF，PDFBox 无法提取文字。

后续方案：

```text
PDF 页面转图片
  ↓
OCR 识别文字
  ↓
合并文字
  ↓
保存 parsedText
```

可选 OCR：

1. Tesseract OCR；
2. 百度 OCR；
3. 阿里云 OCR；
4. PaddleOCR。

第一阶段不建议做 OCR，因为会增加复杂度。

## 31. 面试讲解准备

### 31.1 面试官可能问：你的简历上传是怎么做的？

回答思路：

用户上传 PDF 或 DOCX 文件后，后端先校验文件是否为空、大小是否超过 10MB、扩展名是否为 PDF 或 DOCX。
然后系统会生成新的存储文件名，按 `userId` 分目录保存到本地 `uploads/resumes` 目录下。
保存成功后，根据文件类型调用不同解析器，PDF 使用 PDFBox，DOCX 使用 Apache POI。
解析出的文本经过简单清洗后保存到 `resume` 表的 `parsed_text` 字段，后续 AI 分析模块会读取这个字段。

### 31.2 面试官可能问：为什么不直接用原始文件名保存？

回答思路：

因为原始文件名可能重复，也可能包含中文、特殊字符，甚至路径穿越风险。
所以我只把原始文件名保存到数据库用于展示，实际存储文件名由后端根据 `userId`、时间戳和随机字符串生成。
这样既避免重名，也更安全。

### 31.3 面试官可能问：怎么防止用户访问别人的简历？

回答思路：

JWT 只能证明当前用户是谁，不能自动保证数据权限。
所以所有简历操作都必须带 `userId` 条件。
例如查询简历详情时，不只根据 `resumeId` 查询，而是同时要求 `resume.id = resumeId` 且 `resume.user_id = 当前登录用户ID`。
如果查不到，就返回简历不存在或无权限访问。

### 31.4 面试官可能问：PDF 解析失败怎么办？

回答思路：

PDF 解析失败通常有几种情况：扫描版 PDF、图片型 PDF、加密 PDF 或特殊字体。
第一阶段我会捕获解析异常，返回友好错误提示。
对于扫描版 PDF，后续可以接入 OCR；对于解析文本为空的情况，也可以增加用户手动编辑 `parsedText` 的功能。

### 31.5 面试官可能问：文件上传有什么安全风险？

回答思路：

文件上传主要风险包括大文件攻击、恶意文件上传、路径穿越、文件名污染和隐私泄露。
我的处理方式是限制文件大小，只允许 PDF/DOCX，存储路径由后端配置，存储文件名由系统生成，不使用用户原始文件名作为路径，同时 `uploads` 目录不会提交到 GitHub。

## 32. 开发顺序建议

简历模块建议按以下顺序开发：

1. 创建 `resume` 表；
2. 创建 `Resume` Entity；
3. 创建 `ResumeMapper`；
4. 创建 `FileTypeEnum`；
5. 创建 `ParseStatusEnum`；
6. 创建 `FileStorageProperties`；
7. 配置 `application.yml` 文件上传参数；
8. 创建 `StoredFileInfo`；
9. 创建 `FileStorageService`；
10. 实现 `LocalFileStorageServiceImpl`；
11. 创建 `ResumeParseService`；
12. 实现 `ResumeParseServiceImpl`；
13. 创建 `ResumeUploadResponse`；
14. 创建 `ResumeListResponse`；
15. 创建 `ResumeDetailResponse`；
16. 创建 `ResumeService`；
17. 实现 `ResumeServiceImpl`；
18. 创建 `ResumeController`；
19. 使用 Swagger 上传 PDF 测试；
20. 使用 Swagger 上传 DOCX 测试；
21. 测试简历列表；
22. 测试简历详情；
23. 测试设置默认简历；
24. 测试删除简历；
25. 测试未登录访问返回 401；
26. 测试上传非法文件返回错误。

## 33. 验收标准

### 33.1 上传验收

- 登录用户可以上传 PDF 简历；
- 登录用户可以上传 DOCX 简历；
- 未登录用户不能上传；
- 空文件不能上传；
- 超过 10MB 的文件不能上传；
- 非 PDF/DOCX 文件不能上传；
- 文件保存到本地 `uploads` 目录；
- 数据库保存简历记录；
- 数据库保存 `parsed_text`。

### 33.2 解析验收

- PDF 文本型简历可以解析出文本；
- DOCX 简历可以解析出文本；
- 解析文本不能为空；
- 解析失败时返回友好错误；
- `parsedText` 不在列表接口中完整返回；
- 详情接口可以查看完整 `parsedText`。

### 33.3 数据权限验收

- 用户只能查看自己的简历；
- 用户只能删除自己的简历；
- 用户只能设置自己的简历为默认简历；
- 用户不能通过修改 URL 访问别人的简历；
- 所有查询都包含 `userId` 条件。

### 33.4 默认简历验收

- 用户上传第一份简历时自动设为默认；
- 用户可以手动设置默认简历；
- 同一用户最多只有一份默认简历；
- 删除默认简历后不再显示默认状态。

## 34. 模块设计结论

简历上传与解析模块是 InternPilot 的核心输入模块，负责将用户上传的 PDF 或 DOCX 简历转换为可供 AI 分析使用的文本内容。

第一阶段采用：

```text
本地文件存储 + PDFBox + Apache POI + MySQL 保存 parsedText
```

核心流程为：

```text
用户上传简历
  ↓
校验文件
  ↓
保存文件
  ↓
解析文本
  ↓
清洗文本
  ↓
保存数据库
  ↓
后续 AI 分析使用 parsedText
```

该设计能够满足 MVP 阶段的核心需求，同时保留了后续扩展 MinIO、OCR、简历文本手动编辑、简历版本对比和 AI 简历优化的空间。
