package com.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.dto.AuthRequest;
import com.scheduler.dto.CreateJobRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end slice: register -> login -> create a job -> read it back,
 * running against an in-memory H2 database (see application-test.yml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginAndCreateJob() throws Exception {
        AuthRequest registerRequest = new AuthRequest("integration-user", "s3cret-pass");

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(registerResponse).get("token").asText();

        CreateJobRequest createJobRequest = new CreateJobRequest(
                "nightly-report", "Generates the nightly report", "0 2 * * *",
                "{\"reportType\":\"summary\"}", 2, List.of());

        String createResponse = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nightly-report"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nextRunAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String jobId = objectMapper.readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/jobs/" + jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nightly-report"));
    }

    @Test
    void jobCreationWithoutAuthIsRejected() throws Exception {
        CreateJobRequest createJobRequest = new CreateJobRequest(
                "unauthorized-job", null, "0 * * * *", null, null, List.of());

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createJobRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidCronExpressionIsRejected() throws Exception {
        AuthRequest registerRequest = new AuthRequest("cron-test-user", "s3cret-pass");
        String registerResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        CreateJobRequest badRequest = new CreateJobRequest(
                "broken-job", null, "not-a-cron-expression", null, null, List.of());

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
