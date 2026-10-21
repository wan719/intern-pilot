package com.internpilot.controller.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.internpilot.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ApplicationPlanMysqlSmokeTest {
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("intern_pilot")
            .withInitScript("sql/init.sql");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.sql.init.mode", () -> "never");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @BeforeEach
    void seedSyntheticUsersAndReports() {
        clearSyntheticRows();
        jdbc.update("INSERT INTO user (id, username, password, role) VALUES (9101, 'plan-smoke-a', 'unused', 'USER')");
        jdbc.update("INSERT INTO user (id, username, password, role) VALUES (9102, 'plan-smoke-b', 'unused', 'USER')");
        jdbc.update("INSERT INTO resume (id, user_id, resume_name, original_file_name, stored_file_name, file_path, file_type, file_size) "
                + "VALUES (9201, 9101, '合成简历 A', 'a.pdf', 'a.pdf', '/synthetic/a.pdf', 'PDF', 1), "
                + "(9202, 9102, '合成简历 B', 'b.pdf', 'b.pdf', '/synthetic/b.pdf', 'PDF', 1)");
        jdbc.update("INSERT INTO job_description (id, user_id, company_name, job_title, jd_content) "
                + "VALUES (9301, 9101, '合成公司 A', '合成岗位 A', '合成 JD'), "
                + "(9302, 9102, '合成公司 B', '合成岗位 B', '合成 JD')");
        jdbc.update("INSERT INTO analysis_report (id, user_id, resume_id, job_id, match_score) "
                + "VALUES (9401, 9101, 9201, 9301, 70), (9402, 9102, 9202, 9302, 80)");
    }

    @AfterEach
    void removeSyntheticRows() {
        clearSyntheticRows();
    }

    @Test
    void confirmCreatesOnePendingRecordAndRetryOpensIt() throws Exception {
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "9401")
                        .with(syntheticUser(9101)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("READY"))
                .andExpect(jsonPath("$.data.job.id").value(9301))
                .andExpect(jsonPath("$.data.resume.id").value(9201));
        assertEquals(0, activeCount(9101, 9301));

        String body = "{\"reportId\":9401,\"note\":\"先完善项目经历\"}";
        var created = mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outcome").value("CREATED"))
                .andReturn();
        long applicationId = json.readTree(created.getResponse().getContentAsString())
                .at("/data/applicationId").asLong();
        assertEquals(1, activeCount(9101, 9301));

        mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("EXISTING"))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId));
        assertEquals(1, activeCount(9101, 9301));
        mockMvc.perform(get("/api/applications/" + applicationId).with(syntheticUser(9101)))
                .andExpect(status().isOk());
        assertEquals("TO_APPLY", jdbc.queryForObject(
                "SELECT status FROM application_record WHERE id = ?", String.class, applicationId));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_record WHERE id = ? AND apply_date IS NOT NULL", Integer.class,
                applicationId));
    }

    @Test
    void existingRecordWinsAfterOldResumeIsDeletedWithoutOverwritingFields() throws Exception {
        jdbc.update("INSERT INTO application_record (id, user_id, job_id, resume_id, report_id, status, note, priority) "
                + "VALUES (9501, 9101, 9301, 9201, 9401, 'INTERVIEWING', '原备注', 'HIGH')");
        jdbc.update("UPDATE resume SET deleted = 1 WHERE id = 9201");

        mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":9401,\"note\":\"新备注\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("EXISTING"))
                .andExpect(jsonPath("$.data.applicationId").value(9501));
        assertEquals(1, activeCount(9101, 9301));
        assertEquals("INTERVIEWING|原备注|HIGH", jdbc.queryForObject(
                "SELECT CONCAT(status, '|', note, '|', priority) FROM application_record WHERE id = 9501",
                String.class));
    }

    @Test
    void unavailableResumeWithoutExistingRecordCannotCreateFromReport() throws Exception {
        jdbc.update("UPDATE resume SET deleted = 1 WHERE id = 9201");

        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "9401")
                        .with(syntheticUser(9101)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("RESUME_UNAVAILABLE"));
        mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":9401}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
        assertEquals(0, activeCount(9101, 9301));
    }

    @Test
    void logicallyDeletedHistoryDoesNotBlockAReplacementPlan() throws Exception {
        jdbc.update("INSERT INTO application_record (id, user_id, job_id, resume_id, report_id, status, deleted) "
                + "VALUES (9501, 9101, 9301, 9201, 9401, 'TO_APPLY', 1)");

        mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":9401}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.outcome").value("CREATED"));
        assertEquals(1, activeCount(9101, 9301));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_record WHERE user_id = 9101 AND job_id = 9301", Integer.class));
    }

    @Test
    void anotherUsersReportCannotBeReadOrUsedToCreate() throws Exception {
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "9402")
                        .with(syntheticUser(9101)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        mockMvc.perform(post("/api/applications/from-report")
                        .with(syntheticUser(9101)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":9402}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
        assertEquals(0, activeCount(9101, 9302));
    }

    private RequestPostProcessor syntheticUser(long id) {
        var user = new CustomUserDetails(id, "plan-smoke-" + id, List.of("USER"),
                List.of("application:read", "application:write"));
        return SecurityMockMvcRequestPostProcessors.user(user);
    }

    private int activeCount(long userId, long jobId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM application_record "
                + "WHERE user_id = ? AND job_id = ? AND deleted = 0", Integer.class, userId, jobId);
    }

    private void clearSyntheticRows() {
        jdbc.update("DELETE FROM application_record WHERE user_id IN (9101, 9102)");
        jdbc.update("DELETE FROM analysis_report WHERE id IN (9401, 9402)");
        jdbc.update("DELETE FROM job_description WHERE id IN (9301, 9302)");
        jdbc.update("DELETE FROM resume WHERE id IN (9201, 9202)");
        jdbc.update("DELETE FROM user WHERE id IN (9101, 9102)");
    }
}
