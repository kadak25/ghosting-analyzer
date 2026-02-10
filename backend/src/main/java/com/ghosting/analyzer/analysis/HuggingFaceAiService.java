package com.ghosting.analyzer.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class HuggingFaceAiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.ai.hf.apiKey}")
    private String apiKey;

    @Value("${app.ai.hf.model:mistralai/Mistral-7B-Instruct-v0.3}")
    private String model;

    public String generate(String prompt) {
        String url = "https://router.huggingface.co/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", java.util.List.of(
                        Map.of("role", "system", "content",
                                "Return ONLY valid JSON. No markdown. No extra text."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 900
        );

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        String lastRaw = "";

        for (int attempt = 1; attempt <= 6; attempt++) {
            ResponseEntity<String> res;
            String raw;

            try {
                res = restTemplate.postForEntity(url, req, String.class);
                raw = (res.getBody() == null) ? "" : res.getBody();
                lastRaw = raw;
            } catch (Exception ex) {
                lastRaw = ex.getMessage() == null ? "request exception" : ex.getMessage();
                sleepBackoff(attempt);
                continue;
            }

            if (!res.getStatusCode().is2xxSuccessful()) {
                sleepBackoff(attempt);
                continue;
            }

            if (isTemporaryIssue(raw)) {
                sleepBackoff(attempt);
                continue;
            }

            return extractMessageContent(raw);
        }

        throw new RuntimeException("HF router not ready / temporary errors after retries. lastRaw=" + lastRaw);
    }

    private String extractMessageContent(String rawJson) {
        try {
            JsonNode root = om.readTree(rawJson);

            if (root.has("error")) {
                throw new RuntimeException("HF router error: " + root.get("error").toString());
            }

            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).get("message");
                if (msg != null && msg.has("content")) {
                    return msg.get("content").asText();
                }
            }

            throw new RuntimeException("Unexpected router response format: " + rawJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse router response. Raw=" + rawJson, e);
        }
    }


    private boolean isTemporaryIssue(String raw) {
        if (raw == null) return true;
        String r = raw.toLowerCase();
        return r.contains("rate limit")
                || r.contains("too many requests")
                || r.contains("overloaded")
                || r.contains("currently loading")
                || r.contains("estimated_time");
    }

    private void sleepBackoff(int attempt) {
        try {
            long ms = Math.min(9000L, 1500L * attempt);
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private String extractCompletionText(String rawJson) {
        try {
            JsonNode root = om.readTree(rawJson);

            if (root.has("error")) {
                throw new RuntimeException("HF router error: " + root.get("error").toString());
            }

            // OpenAI completions format: choices[0].text
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode t = choices.get(0).get("text");
                if (t != null) return t.asText();
            }

            throw new RuntimeException("Unexpected completions response format: " + rawJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse completions response. Raw=" + rawJson, e);
        }
    }
}
