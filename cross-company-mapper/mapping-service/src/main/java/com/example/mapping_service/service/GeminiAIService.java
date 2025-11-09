package com.example.mapping_service.service;

import com.example.mapping_service.model.FieldMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    // This service now calls Google Gemini / Generative Language endpoint.
    // Provide full endpoint URL via GEMINI_ENDPOINT env var (e.g., https://generativelanguage.googleapis.com/v1beta2/models/gemini-1.0:generate)
    // Provide API key or bearer token via GEMINI_API_KEY env var.

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<FieldMapping> generateMappings(Map<String, Object> sourceFormat, Map<String, Object> targetFormat) throws Exception {
        String prompt = String.format(
                "Given SOURCE: %s and TARGET: %s, generate JSON array of mappings with sourcePath, targetPath, transformationType (e.g., 'date_format', 'split_name'), and confidence (0-1).",
                objectMapper.writeValueAsString(sourceFormat),
                objectMapper.writeValueAsString(targetFormat)
        );

        String endpoint = System.getenv("GEMINI_ENDPOINT");
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (endpoint == null || endpoint.isEmpty()) {
            throw new IllegalStateException("GEMINI_ENDPOINT not set in environment");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("GEMINI_API_KEY not set in environment");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Accept either API key as Bearer token or raw key depending on your setup
        headers.setBearerAuth(apiKey);

        // Build request body expected by Generative Language API: { "prompt": { "text": "..." }, "temperature": 0.0 }
        Map<String, Object> promptObj = new HashMap<>();
        promptObj.put("text", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("prompt", promptObj);
        body.put("temperature", 0.0);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String resp = restTemplate.postForObject(endpoint, request, String.class);
        JsonNode root = objectMapper.readTree(resp);

        // Try to extract text from common Gemini/Generative Language response shapes
        String content = null;

        // v1beta2 generative responses often include "candidates" array with "content"
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode first = candidates.get(0);
            if (first.has("content")) {
                content = first.path("content").asText();
            }
        }

        // some responses use "output" or "outputs"
        if (content == null) {
            JsonNode outputs = root.path("outputs");
            if (outputs.isArray() && outputs.size() > 0) {
                JsonNode out0 = outputs.get(0);
                if (out0.has("content")) {
                    // content may be array or text
                    JsonNode c = out0.path("content");
                    if (c.isArray() && c.size() > 0) {
                        content = c.get(0).path("text").asText(null);
                    } else if (c.isTextual()) {
                        content = c.asText();
                    }
                }
            }
        }

        // fallback: top-level "response" or "result"
        if (content == null) {
            JsonNode responseNode = root.path("response");
            if (responseNode.isTextual()) {
                content = responseNode.asText();
            } else if (responseNode.has("output")) {
                content = responseNode.path("output").asText(null);
            }
        }

        if (content == null) {
            throw new IllegalStateException("Could not extract generated text from Gemini response: " + resp);
        }

        // Parse JSON array from content into List<FieldMapping>
        return objectMapper.readValue(content, new TypeReference<List<FieldMapping>>(){});
    }
}
