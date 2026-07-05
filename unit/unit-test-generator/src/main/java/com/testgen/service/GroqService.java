package com.testgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to Groq's OpenAI-compatible chat completion endpoint to turn
 * source code into unit test cases.
 */
@Slf4j
@Service
public class GroqService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.model}")
    private String model;

    // Matches ```java ... ``` or ``` ... ``` fenced code blocks
    private static final Pattern CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:[a-zA-Z]+)?\\s*([\\s\\S]*?)```");

    public GroqService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends the source code to Groq and returns clean, ready-to-paste unit test code.
     */
    public String generateUnitTests(String sourceCode, String language, String framework, String testStyle) {
        String prompt = buildPrompt(sourceCode, language, framework, testStyle);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an expert software test engineer. You write complete, "
                                        + "compilable, high-coverage unit tests. You respond ONLY with "
                                        + "a single fenced code block containing the test code — no "
                                        + "explanations before or after."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2,
                "max_tokens", 2048
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return extractTestCode(response.getBody());
        } catch (RestClientException ex) {
            log.error("Groq API call failed", ex);
            throw new RuntimeException("Failed to reach Groq API: " + ex.getMessage(), ex);
        }
    }

    private String buildPrompt(String sourceCode, String language, String framework, String testStyle) {
        return """
                Generate unit tests for the following %s source code using %s.

                %s

                Return ONLY the test code in a single fenced code block. Do not include
                explanations, markdown headers, or commentary outside the code block.

                Source code:
                %s
                """.formatted(language, framework, styleInstructions(testStyle), sourceCode);
    }

    /**
     * Different test styles change what the LLM is asked to prioritize,
     * without changing the overall prompt structure.
     */
    private String styleInstructions(String testStyle) {
        String style = testStyle == null ? "COMPREHENSIVE" : testStyle.toUpperCase();

        return switch (style) {
            case "HAPPY_PATH" -> """
                    Focus ONLY on the happy path: normal, expected inputs that should
                    succeed without errors. Cover the main use cases a typical caller
                    would exercise. Do not test invalid input, exceptions, or edge cases.""";

            case "EDGE_CASES" -> """
                    Focus on exhaustive edge cases: null/empty inputs, unexpected types,
                    exception-triggering conditions, unusual argument combinations,
                    concurrency concerns if relevant, and any state that could break the
                    method in production. Do not spend time on simple, obviously-correct
                    happy-path calls unless needed to set up an edge case.""";

            case "BOUNDARY_VALUES" -> """
                    Focus specifically on boundary value analysis: minimum and maximum
                    valid values, values just inside and just outside valid ranges,
                    zero/negative/off-by-one conditions, and boundary conditions on
                    collection sizes or string lengths. Skip generic happy-path or
                    unrelated edge cases that don't test a boundary.""";

            default -> """
                    Cover normal cases, edge cases, and invalid/exception-triggering
                    inputs for well-rounded coverage. Use proper mocking for external
                    dependencies if any are present. Include clear, descriptive test
                    method names.""";
        };
    }

    /**
     * Three-layer fallback parsing, since LLM output isn't always perfectly formatted:
     *   1. Extract the first fenced code block (```...```) — the expected happy path.
     *   2. If no fenced block exists, fall back to the raw model text as-is.
     *   3. If the API response itself is malformed/unparseable, surface a clear error.
     */
    private String extractTestCode(String rawResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(rawResponseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            Matcher matcher = CODE_BLOCK_PATTERN.matcher(content);
            if (matcher.find()) {
                return matcher.group(1).trim(); // Layer 1: clean fenced code block
            }
            return content.trim(); // Layer 2: raw text, no fencing found

        } catch (Exception e) {
            log.warn("Could not parse Groq response as expected JSON structure", e);
            return "// Unable to parse generated tests automatically.\n"
                    + "// Raw model response has been preserved below for manual review:\n\n"
                    + rawResponseBody; // Layer 3: hardcoded safe fallback
        }
    }
}
