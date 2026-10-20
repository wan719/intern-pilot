package com.internpilot.controller.application;

import com.internpilot.common.ResultCode;
import com.internpilot.dto.application.ApplicationFromReportRequest;
import com.internpilot.exception.BusinessException;
import com.internpilot.service.application.ApplicationService;
import com.internpilot.vo.application.ApplicationFromReportResponse;
import com.internpilot.vo.application.ApplicationPlanContextResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationPlanControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean ApplicationService service;

    @Test
    void anonymousCannotReadPlanContext() throws Exception {
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "30"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(authorities = "job:read")
    void missingReadPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "30"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void anonymousCannotWritePlan() throws Exception {
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":30}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(authorities = "application:read")
    void missingWritePermissionIsForbidden() throws Exception {
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":30}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = "application:read")
    void readyContextReturnsOnlyApprovedSummary() throws Exception {
        ApplicationPlanContextResponse response = new ApplicationPlanContextResponse();
        response.setOutcome("READY");
        response.setReportId(30L);
        when(service.getPlanContext(30L)).thenReturn(response);
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.outcome").value("READY"));
    }

    @Test
    @WithMockUser(authorities = "application:read")
    void invalidOrRepeatedReportIdIsBadRequest() throws Exception {
        for (String invalid : new String[]{"0", "-1", "abc", "9223372036854775808"}) {
            mockMvc.perform(get("/api/applications/plan-context").param("reportId", invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(400));
        }
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "30", "31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(service, never()).getPlanContext(any());
    }

    @Test
    @WithMockUser(authorities = "application:read")
    void inaccessibleReportUsesUniformNotFound() throws Exception {
        when(service.getPlanContext(30L)).thenThrow(
                new BusinessException(ResultCode.NOT_FOUND, "来源不可用或无权访问"));
        mockMvc.perform(get("/api/applications/plan-context").param("reportId", "30"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @WithMockUser(authorities = "application:write")
    void createAndExistingHaveDistinctHttpStatuses() throws Exception {
        when(service.createFromReport(any(ApplicationFromReportRequest.class)))
                .thenReturn(ApplicationFromReportResponse.of("CREATED", 77L))
                .thenReturn(ApplicationFromReportResponse.of("EXISTING", 77L));
        String body = "{\"reportId\":30,\"note\":\"准备完善项目经历\"}";
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicationId").value(77));
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.outcome").value("EXISTING"));
    }

    @Test
    @WithMockUser(authorities = "application:write")
    void clientCannotSupplyDerivedFields() throws Exception {
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":30,\"jobId\":999,\"status\":\"OFFER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(service, never()).createFromReport(any(ApplicationFromReportRequest.class));
    }

    @Test
    @WithMockUser(authorities = "application:write")
    void unrelatedDatabaseConstraintDoesNotLeakSqlOrMasqueradeAsExisting() throws Exception {
        when(service.createFromReport(any(ApplicationFromReportRequest.class)))
                .thenThrow(new DuplicateKeyException("SQL duplicate key secret_parameter"));
        mockMvc.perform(post("/api/applications/from-report").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":30}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("投递计划暂时无法创建，请稍后重试"));
    }
}
