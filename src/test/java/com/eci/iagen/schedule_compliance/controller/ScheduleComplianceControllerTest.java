package com.eci.iagen.schedule_compliance.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceRequest;
import com.eci.iagen.schedule_compliance.dto.ScheduleComplianceResponse;
import com.eci.iagen.schedule_compliance.service.ScheduleComplianceService;

@WebMvcTest(ScheduleComplianceController.class)
public class ScheduleComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private ScheduleComplianceService scheduleComplianceService;

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/schedule-compliance/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Schedule Compliance Service is running"));
    }

    @Test
    public void testEvaluateCompliance() throws Exception {
        // Arrange
        ScheduleComplianceResponse mockResponse = new ScheduleComplianceResponse(
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(5.0),
                3,
                BigDecimal.valueOf(1.5),
                true,
                "{\"evaluationMethod\":\"Days-based penalty system\"}",
                LocalDateTime.now(),
                Arrays.asList());

        when(scheduleComplianceService.evaluateScheduleCompliance(any(ScheduleComplianceRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        String requestJson = """
                {
                    "repositoryUrl": "https://github.com/test/repo",
                    "dueDate": "2024-01-15T23:59:59",
                    "submissionDate": "2024-01-18T10:30:00",
                    "commits": [
                        {
                            "sha": "abc123",
                            "message": "Final submission",
                            "date": "2024-01-18T10:30:00"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/schedule-compliance/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.penalizedScore").value(3.5))
                .andExpect(jsonPath("$.lateDays").value(3))
                .andExpect(jsonPath("$.isLate").value(true));
    }
}
