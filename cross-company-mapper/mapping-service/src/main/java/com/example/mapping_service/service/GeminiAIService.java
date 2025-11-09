package com.example.mapping_service.service;

import com.example.mapping_service.model.FieldMapping;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

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

        // If no external Gemini endpoint/key are configured, fall back to a simple heuristic mapper
        if (endpoint == null || endpoint.isEmpty() || apiKey == null || apiKey.isEmpty()) {
            // simple local heuristic: flatten both formats and match keys by substring/token overlap
            List<FieldMapping> results = new java.util.ArrayList<>();

            Map<String, String> flatSource = new HashMap<>();
            Map<String, String> flatTarget = new HashMap<>();

            // flatten helpers
            flattenMap("", sourceFormat, flatSource);
            flattenMap("", targetFormat, flatTarget);

            // First pass: match each source path to its best target
            java.util.Set<String> mappedTargets = new java.util.HashSet<>();
            for (String sPath : flatSource.keySet()) {
                String bestT = null;
                int bestScore = 0;
                for (String tPath : flatTarget.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestT = tPath;
                    }
                }
                if (bestT != null && bestScore > 0) {
                    String transform = "copy";
                    double confidence = Math.min(0.95, 0.5 + bestScore * 0.12);
                    // small heuristic for date-like fields
                    String lower = sPath.toLowerCase();
                    if (lower.contains("date") || lower.contains("birth") || lower.contains("dob") || lower.contains("issued")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.75);
                    }

                    // heuristic: if source looks like a full name and target has first/last fields,
                    // prefer split_name and targetParent as the parent path
                    String sLow = sPath.toLowerCase();
                    if (sLow.contains("name") || sLow.contains("fullname") || sLow.contains("full_name") || sLow.contains("customername")) {
                        String tLow = bestT.toLowerCase();
                        if (tLow.contains("firstname") || tLow.contains("lastname") || tLow.contains("first") || tLow.contains("last") || tLow.contains("given") || tLow.contains("surname")) {
                            // compute parent path (drop final segment)
                            int idx = bestT.lastIndexOf('.');
                            String parent = idx > 0 ? bestT.substring(0, idx) : bestT;
                            transform = "split_name";
                            confidence = Math.max(confidence, 0.8);
                            results.add(new FieldMapping(sPath, parent, transform, confidence));
                            mappedTargets.add(parent);
                            continue; // skip default add below
                        }
                    }

                    // phone heuristic
                    if (sLow.contains("phone") || sLow.contains("mobile") || sLow.contains("telephone") || sLow.contains("msisdn")) {
                        transform = "phone_format";
                        confidence = Math.max(confidence, 0.7);
                    }

                    results.add(new FieldMapping(sPath, bestT, transform, confidence));
                    mappedTargets.add(bestT);
                }
            }

            // Second pass: ensure targets are covered — for any unmapped target, find the best source
            for (String tPath : flatTarget.keySet()) {
                if (mappedTargets.contains(tPath)) continue;
                String bestS = null;
                int bestScore = 0;
                for (String sPath : flatSource.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestS = sPath;
                    }
                }
                if (bestS != null && bestScore > 0) {
                    double confidence = Math.min(0.7, 0.4 + bestScore * 0.1);
                    String transform = "copy";
                    String sLow = bestS.toLowerCase();
                    if (sLow.contains("date") || sLow.contains("birth") || sLow.contains("dob")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.6);
                    }
                    if (sLow.contains("name") && (tPath.toLowerCase().contains("first") || tPath.toLowerCase().contains("last"))) {
                        // if target looks like first/last and source is full name, create split mapping to parent
                        int idx = tPath.lastIndexOf('.');
                        String parent = idx > 0 ? tPath.substring(0, idx) : tPath;
                        results.add(new FieldMapping(bestS, parent, "split_name", Math.max(confidence, 0.6)));
                        mappedTargets.add(tPath);
                        continue;
                    }
                    results.add(new FieldMapping(bestS, tPath, transform, confidence));
                    mappedTargets.add(tPath);
                }
            }

                return results;
            }

    HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Accept either API key as Bearer token or raw key depending on your setup
    // Log that we are about to call the external endpoint (do not log the API key)
    logger.info("GeminiAIService: calling external Gemini endpoint: {}", endpoint);
    headers.setBearerAuth(apiKey);

    // Build request body in the Quickstart format used by Google AI Studio:
    // { "contents": [ { "parts": [ { "text": "..." } ] } ], "temperature": 0.0 }
    Map<String, Object> part = new HashMap<>();
    part.put("text", prompt);
    java.util.List<Map<String, Object>> parts = new java.util.ArrayList<>();
    parts.add(part);
    Map<String, Object> contentItem = new HashMap<>();
    contentItem.put("parts", parts);
    java.util.List<Map<String, Object>> contents = new java.util.ArrayList<>();
    contents.add(contentItem);

    Map<String, Object> body = new HashMap<>();
    body.put("contents", contents);

    // Use API key header expected by the quickstart example
    headers.remove(HttpHeaders.AUTHORIZATION);
    headers.add("X-goog-api-key", apiKey);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        String resp = null;
        try {
            resp = restTemplate.postForObject(endpoint, request, String.class);
        } catch (HttpClientErrorException | ResourceAccessException ex) {
            logger.warn("GeminiAPI call failed: {} - falling back to heuristic.", ex.getMessage());
        } catch (Exception ex) {
            logger.warn("Unexpected error calling Gemini API: {} - falling back to heuristic.", ex.getMessage());
        }

        if (resp == null || resp.trim().isEmpty()) {
            logger.info("GeminiAIService: empty or failed response, using local heuristic fallback.");
            // local fallback (re-run heuristic matching) — same approach as when env vars are missing
            List<FieldMapping> results = new java.util.ArrayList<>();

            Map<String, String> flatSource2 = new HashMap<>();
            Map<String, String> flatTarget2 = new HashMap<>();
            flattenMap("", sourceFormat, flatSource2);
            flattenMap("", targetFormat, flatTarget2);

            java.util.Set<String> mappedTargets = new java.util.HashSet<>();
            for (String sPath : flatSource2.keySet()) {
                String bestT = null;
                int bestScore = 0;
                for (String tPath : flatTarget2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestT = tPath;
                    }
                }
                if (bestT != null && bestScore > 0) {
                    String transform = "copy";
                    double confidence = Math.min(0.95, 0.5 + bestScore * 0.12);
                    String lower = sPath.toLowerCase();
                    if (lower.contains("date") || lower.contains("birth") || lower.contains("dob") || lower.contains("issued")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.75);
                    }
                    String sLow = sPath.toLowerCase();
                    if (sLow.contains("name") || sLow.contains("fullname") || sLow.contains("full_name") || sLow.contains("customername")) {
                        String tLow = bestT.toLowerCase();
                        if (tLow.contains("firstname") || tLow.contains("lastname") || tLow.contains("first") || tLow.contains("last") || tLow.contains("given") || tLow.contains("surname")) {
                            int idx = bestT.lastIndexOf('.');
                            String parent = idx > 0 ? bestT.substring(0, idx) : bestT;
                            transform = "split_name";
                            confidence = Math.max(confidence, 0.8);
                            results.add(new FieldMapping(sPath, parent, transform, confidence));
                            mappedTargets.add(parent);
                            continue;
                        }
                    }
                    if (sLow.contains("phone") || sLow.contains("mobile") || sLow.contains("telephone") || sLow.contains("msisdn")) {
                        transform = "phone_format";
                        confidence = Math.max(confidence, 0.7);
                    }
                    results.add(new FieldMapping(sPath, bestT, transform, confidence));
                    mappedTargets.add(bestT);
                }
            }
            for (String tPath : flatTarget2.keySet()) {
                if (mappedTargets.contains(tPath)) continue;
                String bestS = null;
                int bestScore = 0;
                for (String sPath : flatSource2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestS = sPath;
                    }
                }
                if (bestS != null && bestScore > 0) {
                    double confidence = Math.min(0.7, 0.4 + bestScore * 0.1);
                    String transform = "copy";
                    String sLow = bestS.toLowerCase();
                    if (sLow.contains("date") || sLow.contains("birth") || sLow.contains("dob")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.6);
                    }
                    if (sLow.contains("name") && (tPath.toLowerCase().contains("first") || tPath.toLowerCase().contains("last"))) {
                        int idx = tPath.lastIndexOf('.');
                        String parent = idx > 0 ? tPath.substring(0, idx) : tPath;
                        results.add(new FieldMapping(bestS, parent, "split_name", Math.max(confidence, 0.6)));
                        mappedTargets.add(tPath);
                        continue;
                    }
                    results.add(new FieldMapping(bestS, tPath, transform, confidence));
                    mappedTargets.add(tPath);
                }
            }
            return results;
        }

        JsonNode root = objectMapper.readTree(resp);

        // Try to extract text from common Gemini/Generative Language response shapes
        String content = null;

        // v1beta2 generative responses often include "candidates" array with "content"
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode first = candidates.get(0);
            if (first.has("content")) {
                content = first.path("content").asText(null);
            } else if (first.has("output")) {
                content = first.path("output").asText(null);
            }
        }

        // some responses use "outputs" or nested content arrays
        if (content == null) {
            JsonNode outputs = root.path("outputs");
            if (outputs.isArray() && outputs.size() > 0) {
                JsonNode out0 = outputs.get(0);
                if (out0.has("content")) {
                    JsonNode c = out0.path("content");
                    if (c.isArray() && c.size() > 0) {
                        JsonNode firstPart = c.get(0);
                        if (firstPart.has("text")) {
                            content = firstPart.path("text").asText(null);
                        } else {
                            content = firstPart.asText(null);
                        }
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

        if (content == null || content.trim().isEmpty()) {
            logger.warn("Could not extract generated text from Gemini response or content was empty; falling back to heuristic.");
            // reuse the heuristic fallback (same as earlier)
            Map<String, String> flatSource2 = new HashMap<>();
            Map<String, String> flatTarget2 = new HashMap<>();
            flattenMap("", sourceFormat, flatSource2);
            flattenMap("", targetFormat, flatTarget2);

            java.util.List<FieldMapping> results = new java.util.ArrayList<>();
            java.util.Set<String> mappedTargets = new java.util.HashSet<>();
            for (String sPath : flatSource2.keySet()) {
                String bestT = null;
                int bestScore = 0;
                for (String tPath : flatTarget2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestT = tPath;
                    }
                }
                if (bestT != null && bestScore > 0) {
                    String transform = "copy";
                    double confidence = Math.min(0.95, 0.5 + bestScore * 0.12);
                    String lower = sPath.toLowerCase();
                    if (lower.contains("date") || lower.contains("birth") || lower.contains("dob") || lower.contains("issued")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.75);
                    }
                    String sLow = sPath.toLowerCase();
                    if (sLow.contains("name") || sLow.contains("fullname") || sLow.contains("full_name") || sLow.contains("customername")) {
                        String tLow = bestT.toLowerCase();
                        if (tLow.contains("firstname") || tLow.contains("lastname") || tLow.contains("first") || tLow.contains("last") || tLow.contains("given") || tLow.contains("surname")) {
                            int idx = bestT.lastIndexOf('.');
                            String parent = idx > 0 ? bestT.substring(0, idx) : bestT;
                            transform = "split_name";
                            confidence = Math.max(confidence, 0.8);
                            results.add(new FieldMapping(sPath, parent, transform, confidence));
                            mappedTargets.add(parent);
                            continue;
                        }
                    }
                    if (sLow.contains("phone") || sLow.contains("mobile") || sLow.contains("telephone") || sLow.contains("msisdn")) {
                        transform = "phone_format";
                        confidence = Math.max(confidence, 0.7);
                    }
                    results.add(new FieldMapping(sPath, bestT, transform, confidence));
                    mappedTargets.add(bestT);
                }
            }
            for (String tPath : flatTarget2.keySet()) {
                if (mappedTargets.contains(tPath)) continue;
                String bestS = null;
                int bestScore = 0;
                for (String sPath : flatSource2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestS = sPath;
                    }
                }
                if (bestS != null && bestScore > 0) {
                    double confidence = Math.min(0.7, 0.4 + bestScore * 0.1);
                    String transform = "copy";
                    String sLow = bestS.toLowerCase();
                    if (sLow.contains("date") || sLow.contains("birth") || sLow.contains("dob")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.6);
                    }
                    if (sLow.contains("name") && (tPath.toLowerCase().contains("first") || tPath.toLowerCase().contains("last"))) {
                        int idx = tPath.lastIndexOf('.');
                        String parent = idx > 0 ? tPath.substring(0, idx) : tPath;
                        results.add(new FieldMapping(bestS, parent, "split_name", Math.max(confidence, 0.6)));
                        mappedTargets.add(tPath);
                        continue;
                    }
                    results.add(new FieldMapping(bestS, tPath, transform, confidence));
                    mappedTargets.add(tPath);
                }
            }
            return results;
        }

        // Try parsing content as JSON array of mappings, otherwise fall back
        try {
            return objectMapper.readValue(content, new TypeReference<List<FieldMapping>>(){});
        } catch (Exception ex) {
            logger.warn("Failed to parse Gemini content as JSON mappings: {}. Falling back to heuristic.", ex.getMessage());
            // fallback heuristic (same as above)
            Map<String, String> flatSource2 = new HashMap<>();
            Map<String, String> flatTarget2 = new HashMap<>();
            flattenMap("", sourceFormat, flatSource2);
            flattenMap("", targetFormat, flatTarget2);
            java.util.List<FieldMapping> results = new java.util.ArrayList<>();
            java.util.Set<String> mappedTargets = new java.util.HashSet<>();
            for (String sPath : flatSource2.keySet()) {
                String bestT = null;
                int bestScore = 0;
                for (String tPath : flatTarget2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestT = tPath;
                    }
                }
                if (bestT != null && bestScore > 0) {
                    String transform = "copy";
                    double confidence = Math.min(0.95, 0.5 + bestScore * 0.12);
                    String lower = sPath.toLowerCase();
                    if (lower.contains("date") || lower.contains("birth") || lower.contains("dob") || lower.contains("issued")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.75);
                    }
                    String sLow = sPath.toLowerCase();
                    if (sLow.contains("name") || sLow.contains("fullname") || sLow.contains("full_name") || sLow.contains("customername")) {
                        String tLow = bestT.toLowerCase();
                        if (tLow.contains("firstname") || tLow.contains("lastname") || tLow.contains("first") || tLow.contains("last") || tLow.contains("given") || tLow.contains("surname")) {
                            int idx = bestT.lastIndexOf('.');
                            String parent = idx > 0 ? bestT.substring(0, idx) : bestT;
                            transform = "split_name";
                            confidence = Math.max(confidence, 0.8);
                            results.add(new FieldMapping(sPath, parent, transform, confidence));
                            mappedTargets.add(parent);
                            continue;
                        }
                    }
                    if (sLow.contains("phone") || sLow.contains("mobile") || sLow.contains("telephone") || sLow.contains("msisdn")) {
                        transform = "phone_format";
                        confidence = Math.max(confidence, 0.7);
                    }
                    results.add(new FieldMapping(sPath, bestT, transform, confidence));
                    mappedTargets.add(bestT);
                }
            }
            for (String tPath : flatTarget2.keySet()) {
                if (mappedTargets.contains(tPath)) continue;
                String bestS = null;
                int bestScore = 0;
                for (String sPath : flatSource2.keySet()) {
                    int score = scoreSimilarity(sPath, tPath);
                    if (score > bestScore) {
                        bestScore = score;
                        bestS = sPath;
                    }
                }
                if (bestS != null && bestScore > 0) {
                    double confidence = Math.min(0.7, 0.4 + bestScore * 0.1);
                    String transform = "copy";
                    String sLow = bestS.toLowerCase();
                    if (sLow.contains("date") || sLow.contains("birth") || sLow.contains("dob")) {
                        transform = "date_format";
                        confidence = Math.max(confidence, 0.6);
                    }
                    if (sLow.contains("name") && (tPath.toLowerCase().contains("first") || tPath.toLowerCase().contains("last"))) {
                        int idx = tPath.lastIndexOf('.');
                        String parent = idx > 0 ? tPath.substring(0, idx) : tPath;
                        results.add(new FieldMapping(bestS, parent, "split_name", Math.max(confidence, 0.6)));
                        mappedTargets.add(tPath);
                        continue;
                    }
                    results.add(new FieldMapping(bestS, tPath, transform, confidence));
                    mappedTargets.add(tPath);
                }
            }
                return results;
            }

        }

        // Utility: flatten nested maps into dot.paths -> valueString
    private void flattenMap(String prefix, Map<String, Object> map, Map<String, String> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            Object v = e.getValue();
            if (v instanceof Map) {
                // unchecked cast but fine for demo
                flattenMap(key, (Map<String, Object>) v, out);
            } else {
                out.put(key, v == null ? "" : v.toString());
            }
        }
    }

    // very basic similarity: count common tokens
    private int scoreSimilarity(String a, String b) {
        java.util.Set<String> sa = new java.util.HashSet<>();
        for (String t : tokenize(a)) if (!t.isEmpty()) sa.add(t);
        int score = 0;
        for (String t : tokenize(b)) if (!t.isEmpty()) {
            if (sa.contains(t)) {
                score++;
            } else {
                // partial match: token contains or is contained
                for (String s : sa) {
                    if (s.contains(t) || t.contains(s)) {
                        score++;
                        break;
                    }
                }
            }
        }
        return score;
    }

    private String[] tokenize(String s) {
        if (s == null) return new String[0];
        // split camelCase boundaries: 'firstName' -> 'first Name'
        String spaced = s.replaceAll("([a-z])([A-Z])", "$1 $2");
        // replace non-alnum with spaces, and split on dots and underscores
        spaced = spaced.replaceAll("[._\\-]+", " ");
        spaced = spaced.replaceAll("[^A-Za-z0-9 ]", " ");
        String[] parts = spaced.toLowerCase().split("\\s+");
        // additionally split tokens on numbers/letters boundaries if needed
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            // try to split camel-case-like tokens further by letter/number boundaries
            String t = p.replaceAll("([0-9]+)", " $1 ").trim();
            for (String sub : t.split("\\s+")) if (!sub.isEmpty()) out.add(sub);
        }
        return out.toArray(new String[0]);
    }
}
