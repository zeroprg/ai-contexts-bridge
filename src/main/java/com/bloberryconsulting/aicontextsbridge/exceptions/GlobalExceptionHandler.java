package com.bloberryconsulting.aicontextsbridge.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.json.JSONObject;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.TooManyRequests.class)
    public ResponseEntity<String> handleTooManyRequestsException(HttpClientErrorException.TooManyRequests ex) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("message", "You exceeded your current quota, please check your plan and billing details.");
        errorResponse.put("type", "insufficient_quota");
        errorResponse.put("code", "insufficient_quota");
        errorResponse.put("recomendation", "Please use select a different API from the list of available APIs.");

        return new ResponseEntity<>(errorResponse.toString(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientErrorException(HttpClientErrorException ex) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("message",  ex.getLocalizedMessage());
        errorResponse.put("type", "http client error");
        errorResponse.put("code", ex.getStatusCode().toString());
        errorResponse.put("recomendation", "Please cut context.");

        return new ResponseEntity<>(errorResponse.toString(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(APIError.class)
    public ResponseEntity<String> handleAPIError(APIError ex) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("message", ex.getLocalizedMessage());
        errorResponse.put("type", "Internal Error");
        errorResponse.put("code", "500");
        errorResponse.put("recomendation", "Please select any available API from the list of APIs.");

        return new ResponseEntity<>(errorResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ... Other exception handlers
}
