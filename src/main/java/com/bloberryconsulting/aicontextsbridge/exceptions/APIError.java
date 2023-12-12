package com.bloberryconsulting.aicontextsbridge.exceptions;

import org.springframework.http.HttpStatus;

public class APIError extends RuntimeException {

    private final HttpStatus statusCode;

    public APIError(HttpStatus badRequest, String message) {
        super(message);
        this.statusCode = badRequest;
    }

    public int getStatusCode() {
        return statusCode.value();
    }
}
