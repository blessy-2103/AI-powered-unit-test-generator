package com.testgen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResponse {
    private Long id;
    private String className;
    private String generatedTests;
    private String language;
    private String framework;
    private String testStyle;
    private boolean success;
    private String errorMessage;

    public static GenerateResponse ofError(String message) {
        GenerateResponse r = new GenerateResponse();
        r.setSuccess(false);
        r.setErrorMessage(message);
        return r;
    }
}
