package com.gjira.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class E2EIngestTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void webhookSlack_acceptsAndReturns202() throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.set("event", objectMapper.createObjectNode()
                .put("text", "Deploy to prod completed.")
                .put("channel_id", "C123")
                .put("ts", "1234567890.123456"));

        mvc.perform(post("/webhook/slack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .content(objectMapper.writeValueAsString(root)))
                .andExpect(status().isAccepted());
    }

    @Test
    void webhookConfluence_acceptsAndReturns202() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("id", "12345")
                .putObject("body")
                .putObject("storage")
                .put("value", "<p>Runbook for incident response.</p>");

        payload.putObject("body").putObject("storage").put("value", "<p>Runbook for incident response.</p>");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", "12345");
        root.putObject("body").putObject("storage").put("value", "<p>Runbook for incident response.</p>");

        mvc.perform(post("/webhook/confluence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .content(objectMapper.writeValueAsString(root)))
                .andExpect(status().isAccepted());
    }

    @Test
    void staticUploadText_acceptsAndReturns202() throws Exception {
        ObjectNode body = objectMapper.createObjectNode()
                .put("text", "Policy: All deployments must pass CI.")
                .put("sourceId", "policy-001");

        mvc.perform(post("/ingest/static/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());
    }

    @Test
    void staticUploadFile_acceptsAndReturns202() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.txt",
                MediaType.TEXT_PLAIN_VALUE, "Static library content for E2E.".getBytes());

        mvc.perform(multipart("/ingest/static/upload")
                        .file(file)
                        .header("X-Tenant-Id", "00000000-0000-0000-0000-000000000001")
                        .param("sourceId", "upload-e2e-1"))
                .andExpect(status().isAccepted());
    }

    @Test
    void health_returnsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
