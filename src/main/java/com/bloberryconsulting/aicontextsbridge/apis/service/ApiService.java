package com.bloberryconsulting.aicontextsbridge.apis.service;


public interface ApiService {
    String getResponse(String userId, String apiKey, String apiUri, String message);
    String getApiId();
}