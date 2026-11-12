package com.internpilot.controller.application;

import com.internpilot.dto.application.ApplicationPriorityUpdateRequest;
import com.internpilot.service.application.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationPriorityControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean ApplicationService service;

    @Test
    void anonymousCannotEditPriority() throws Exception {
        mockMvc.perform(put("/api/applications/17/priority").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\"}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).updatePriority(eq(17L), any());
    }

    @Test
    @WithMockUser(authorities = "application:read")
    void readOnlyUserCannotEditPriority() throws Exception {
        mockMvc.perform(put("/api/applications/17/priority").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\"}"))
                .andExpect(status().isForbidden());
        verify(service, never()).updatePriority(eq(17L), any());
    }

    @Test
    @WithMockUser(authorities = "application:write")
    void authorizedUserCanEditPriority() throws Exception {
        when(service.updatePriority(eq(17L), any(ApplicationPriorityUpdateRequest.class))).thenReturn(true);
        mockMvc.perform(put("/api/applications/17/priority").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
        verify(service).updatePriority(eq(17L), any(ApplicationPriorityUpdateRequest.class));
    }

    @Test
    @WithMockUser(authorities = "application:write")
    void missingOrBlankPriorityIsRejectedBeforeServiceCall() throws Exception {
        for (String body : new String[]{"{}", "{\"priority\":null}", "{\"priority\":\"\"}", "{\"priority\":\" \"}"}) {
            mockMvc.perform(put("/api/applications/17/priority").contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400));
        }
        verify(service, never()).updatePriority(eq(17L), any());
    }
}
