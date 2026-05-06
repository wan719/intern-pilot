package com.internpilot.service.impl;

import com.internpilot.exception.FileParseException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeParseServiceImplTest {

    private final ResumeParseServiceImpl service = new ResumeParseServiceImpl();

    @Test
    void shouldParseDocxText() throws IOException {
        byte[] docxBytes = createDocx("Java Spring Boot", "Redis MySQL");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        String parsed = service.parse(file, "DOCX");

        assertTrue(parsed.contains("Java Spring Boot"));
        assertTrue(parsed.contains("Redis MySQL"));
    }

    @Test
    void shouldNormalizeWhitespace() throws IOException {
        byte[] docxBytes = createDocx("Java    Spring", "", "Redis");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        String parsed = service.parse(file, "DOCX");

        assertEquals("Java Spring\nRedis", parsed);
    }

    @Test
    void shouldRejectUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "abc".getBytes());
        assertThrows(FileParseException.class, () -> service.parse(file, "TXT"));
    }

    private byte[] createDocx(String... paragraphs) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (String paragraphText : paragraphs) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(paragraphText);
            }
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
