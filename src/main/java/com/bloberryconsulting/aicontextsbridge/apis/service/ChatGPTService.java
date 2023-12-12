package com.bloberryconsulting.aicontextsbridge.apis.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


import org.json.JSONObject;

@Service
public class ChatGPTService implements ApiService {

    // private final String openAIEndpoint = 
    // "https://api.openai.com/v1/engines/davinci/completions";

    private final int MAX_TOKEN = 150;  // You can adjust the number of tokens

    // Implementation for ChatGPT
    @Override
    public String getResponse(String userId, String apiKey, String apiUri, String userInput) {
        // Create a request body
        JSONObject body = new JSONObject();
        body.put("prompt", userInput);
        body.put("max_tokens", MAX_TOKEN); // You can adjust the number of tokens

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Create a request entity
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        // Use RestTemplate to send a POST request
        RestTemplate restTemplate = new RestTemplate();
        String responseBody;

        // Send a POST request to ChatGPT
        ResponseEntity<String> response = restTemplate.postForEntity(apiUri, request, String.class);
        responseBody = response.getBody();

        // Return the response from ChatGPT
        return responseBody;
    }
    public String getApiId() {
        return "ChatGPT";
    }
}
