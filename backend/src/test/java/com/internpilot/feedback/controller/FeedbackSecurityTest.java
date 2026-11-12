package com.internpilot.feedback.controller;

import com.internpilot.feedback.service.FeedbackService;
import com.internpilot.feedback.vo.FeedbackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeedbackSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @Test
    void anonymousUserCannotListOwnFeedback() throws Exception {
        mockMvc.perform(get("/api/feedback/my"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @WithMockUser(authorities = "job:read")
    void userWithoutFeedbackReadCannotListAdminFeedback() throws Exception {
        mockMvc.perform(get("/api/admin/feedback"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(feedbackService);
    }

    @Test
    @WithMockUser(authorities = "feedback:read")
    void userWithFeedbackReadCanListAdminFeedback() throws Exception {
        FeedbackResponse feedback = new FeedbackResponse();
        feedback.setId(42L);
        when(feedbackService.listAllFeedbacks(null, null)).thenReturn(List.of(feedback));

        mockMvc.perform(get("/api/admin/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(42));
    }
}
