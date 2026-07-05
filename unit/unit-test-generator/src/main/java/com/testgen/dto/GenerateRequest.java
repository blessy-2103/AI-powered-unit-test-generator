package com.testgen.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateRequest {

    @NotBlank(message = "Source code must not be empty")
    private String sourceCode;

    // e.g. "Java", "Python", "JavaScript" - defaults to Java if not provided
    private String language = "Java";

    // e.g. "JUnit 5", "Mockito", "pytest" - defaults to JUnit 5
    private String framework = "JUnit 5";

    // "COMPREHENSIVE" (default), "HAPPY_PATH", "EDGE_CASES", or "BOUNDARY_VALUES"
    private String testStyle = "COMPREHENSIVE";

    // Optional: className hint, if left blank the LLM/service will infer it
    private String className;
}
