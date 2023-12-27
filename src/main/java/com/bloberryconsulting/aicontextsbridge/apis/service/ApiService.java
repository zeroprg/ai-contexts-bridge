package com.bloberryconsulting.aicontextsbridge.apis.service;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;

public interface ApiService {
    String getResponse(ApiKey apiKey, String message);
    String getApiId();
}