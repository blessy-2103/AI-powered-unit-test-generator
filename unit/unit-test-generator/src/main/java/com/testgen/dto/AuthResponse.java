package com.testgen.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private boolean success;
    private String errorMessage;

    public static AuthResponse ofError(String message) {
        AuthResponse r = new AuthResponse();
        r.setSuccess(false);
        r.setErrorMessage(message);
        return r;
    }

    public static AuthResponse ofSuccess(String token, String username) {
        return new AuthResponse(token, username, true, null);
    }
}
