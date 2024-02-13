package com.bloberryconsulting.aicontextsbridge.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

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
        errorResponse.put("message", ex.getLocalizedMessage());
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
        errorResponse.put("code", ex.getStatusCode());
        errorResponse.put("recomendation", "Please cut context.");

        return new ResponseEntity<>(errorResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public void handleWebSocketException(WebSocketSession session, Exception ex) throws IOException {
        // Handle the exception as needed, similar to how you handle exceptions in
        // controllers
        // You might need to manually send an error message to the WebSocket client
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(ex.getLocalizedMessage()));           
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        JSONObject errorResponse = new JSONObject();
        errorResponse.put("message", ex.getLocalizedMessage().split(":")[2]);
        errorResponse.put("type", "Internal Error");
        errorResponse.put("code", HttpStatus.INTERNAL_SERVER_ERROR);
        errorResponse.put("recomendation", "Please use smaller file.");
        return new ResponseEntity<>(errorResponse.toString(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    // ... Other exception handlers

}
