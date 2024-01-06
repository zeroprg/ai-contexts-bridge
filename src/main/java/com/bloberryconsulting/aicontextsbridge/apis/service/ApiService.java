package com.bloberryconsulting.aicontextsbridge.apis.service;


import java.util.List;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;

public interface ApiService {
    String getResponse(ApiKey apiKey, String message, List<Context>  contextHistory);
    String getApiId();
}