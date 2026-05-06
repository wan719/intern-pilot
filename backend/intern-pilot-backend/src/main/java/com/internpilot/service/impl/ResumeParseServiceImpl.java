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
                    builder.append(text).append('\n');
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
