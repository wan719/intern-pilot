package com.internpilot.resume.controller;

import com.internpilot.auth.security.CustomUserDetails;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ContentDisposition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResumeOriginalFileTest {

    @TempDir
    static Path uploads;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("file.upload-dir", () -> uploads.resolve("resumes").toString());
        registry.add("file.avatar-dir", () -> uploads.resolve("avatars").toString());
    }

    private static final String FILE = "/api/resumes/9211/file";
    private static final long OWNER = 9131L;
    private static final String PDF_NAME = "校园开发简历.pdf";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private Path pdf;
    private Path docx;

    @BeforeEach
    void fixtures() throws Exception {
        Path ownerDir = Files.createDirectories(uploads.resolve("resumes/user-" + OWNER));
        pdf = ownerDir.resolve("historical.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }
        docx = ownerDir.resolve("historical.docx");
        try (XWPFDocument document = new XWPFDocument(); var out = Files.newOutputStream(docx)) {
            document.createParagraph().createRun().setText("Synthetic resume");
            document.write(out);
        }
        Files.createDirectories(uploads.resolve("avatars/user-" + OWNER));
        Files.write(uploads.resolve("avatars/user-" + OWNER + "/test.png"),
                new byte[]{(byte) 0x89, 'P', 'N', 'G'});

        insert(9211L, PDF_NAME, "historical.pdf", "PDF", 0);
        insert(9212L, "项目简历.docx", "historical.docx", "DOCX", 0);
        insert(9213L, PDF_NAME, "absent.pdf", "PDF", 0);
        insert(9214L, PDF_NAME, "historical.pdf", "PDF", 1);
        insert(9215L, PDF_NAME, "resumes/historical.pdf", "PDF", 0);
    }

    @Test
    void ownerCanReadOriginalFilesFromTheCurrentMount() throws Exception {
        mvc.perform(get(FILE).with(account(OWNER, "resume:read")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(Files.readAllBytes(pdf)))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(result -> assertEquals(PDF_NAME, ContentDisposition.parse(
                        result.getResponse().getHeader("Content-Disposition")).getFilename()));
        mvc.perform(get("/api/resumes/9212/file").with(account(OWNER, "resume:read")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(content().bytes(Files.readAllBytes(docx)));
        mvc.perform(get("/api/resumes/9211").with(account(OWNER, "resume:read")))
                .andExpect(jsonPath("$.data.storedFileName").doesNotExist())
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.parsedText").value("AI text"));
    }

    @Test
    void blocksUnauthorizedAndUnavailableOriginals() throws Exception {
        mvc.perform(get(FILE)).andExpect(status().isUnauthorized());
        mvc.perform(get(FILE).with(account(OWNER, "job:read"))).andExpect(status().isForbidden());
        mvc.perform(get(FILE).with(account(OWNER + 1, "resume:read"))).andExpect(status().isNotFound());
        for (long id : List.of(9213L, 9214L, 9215L)) {
            mvc.perform(get("/api/resumes/" + id + "/file").with(account(OWNER, "resume:read")))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void onlyAvatarsAreAvailableThroughPublicUploads() throws Exception {
        String oldPath = "/uploads/resumes/user-" + OWNER + "/historical.pdf";
        mvc.perform(get(oldPath)).andExpect(status().isUnauthorized());
        mvc.perform(get(oldPath).with(account(OWNER, "resume:read"))).andExpect(status().isForbidden());
        mvc.perform(get("/uploads/avatars/user-" + OWNER + "/test.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{(byte) 0x89, 'P', 'N', 'G'}));
    }

    private RequestPostProcessor account(long id, String authority) {
        return user(new CustomUserDetails(id, "synthetic-" + id, List.of(), List.of(authority)));
    }

    private void insert(long id, String originalName, String storedName, String fileType, int deleted) {
        jdbc.update("""
                INSERT INTO resume (id, user_id, original_file_name, stored_file_name, file_path, file_type, parsed_text, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, OWNER, originalName, storedName, "legacy-root/" + storedName, fileType, "AI text", deleted);
    }
}
