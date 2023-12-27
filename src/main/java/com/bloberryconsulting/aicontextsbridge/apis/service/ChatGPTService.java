package com.bloberryconsulting.aicontextsbridge.apis.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;

import org.json.JSONArray;
import org.json.JSONObject;

@Service
public class ChatGPTService implements ApiService {

    private final static double RESPONSE_LENGTH_RATIO = 0.30; // 30% of the model's maximum token limit

    private int calculateDesiredResponseLength(int maxModelTokens) {
        return (int) Math.round(maxModelTokens * RESPONSE_LENGTH_RATIO);
    }

    private int calculateMaxTokens(int maxModelTokens, int inputTokens) {
        int desiredResponseLength = calculateDesiredResponseLength(maxModelTokens);

        // Decide on a reasonable maximum limit for the response length
        // This is to prevent the model from generating an extremely long response
        int maxResponseTokens = maxModelTokens - inputTokens;

        // Ensure the maximum response tokens do not exceed the desired length
        return Math.min(maxResponseTokens, desiredResponseLength);
    }

    // Implementation for ChatGPT
    @Override
    public String getResponse(ApiKey apiKey, String userInput) {
        try {
            if (apiKey.getUri().contains("/chat/completions")) {
                return getChatResponse(apiKey, userInput);
            } else {
                return getCompletionResponse(apiKey, userInput);
            }
        } catch (HttpClientErrorException e) {
            throw new APIError(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        }
    }
    private String extractModelFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            int modelIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("engines".equals(parts[i])) {
                    modelIndex = i + 1;
                    break;
                }
            }
            if (modelIndex >= 0 && modelIndex < parts.length) {
                return parts[modelIndex];
            }
            return ""; // Or you can return null or throw an exception based on your error handling strategy
        } catch (Exception e) {
            // Handle any unexpected errors during parsing
            return "";
        }
    }
    

    private String getChatResponse(ApiKey apiKey, String userInput) {
        // Extracting the model name from the URI
        String modelName = apiKey.getModel();   //extractModelFromUri(apiKey.getUri());
        if (modelName == null || modelName.isEmpty()) {
            throw new APIError(HttpStatus.BAD_REQUEST, "Model name could not be extracted from the URI.");
        }
        // Prepare the request body with necessary parameters
        JSONObject body = new JSONObject();
        body.put("model",modelName); // Assuming ApiKey class has a getModel method
    
        JSONArray messages = new JSONArray();
        // Optionally, you can add a system message for context, if needed
        // messages.put(new JSONObject().put("role", "system").put("content", "Your system message here"));
    
        // Adding the user's message
        messages.put(new JSONObject().put("role", "user").put("content", userInput));
        body.put("messages", messages);
    
        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.getKeyValue());
    
        // Create a request entity
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
    
        // Initialize RestTemplate for the HTTP request
        RestTemplate restTemplate = new RestTemplate();
        String responseBody;
    
        try {
            // Send a POST request to the OpenAI Chat API
            ResponseEntity<String> response = restTemplate.postForEntity(apiKey.getUri(), request, String.class);
            responseBody = response.getBody();
    
            // Handle HTTP status codes
            if (response.getStatusCode().is2xxSuccessful()) {
                // Extract and return the response text
                return extractTextFromChatResponse(responseBody);
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "API Error: " + responseBody);
            }
        } catch (HttpClientErrorException e) {
            throw new APIError(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
    
    private String extractTextFromChatResponse(String responseBody) {
        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray choices = jsonResponse.getJSONArray("choices");
    
        // StringBuilder to concatenate text from all assistant responses
        StringBuilder concatenatedText = new StringBuilder();
    
        for (int i = 0; i < choices.length(); i++) {
            JSONObject choice = choices.getJSONObject(i);
            if (choice.has("message")) {  // Check if the choice has a message
                JSONObject message = choice.getJSONObject("message");
                if (message.getString("role").equals("assistant")) {
                    concatenatedText.append(message.getString("content")).append("\n");
                }
            }
        }
    
        return concatenatedText.toString();
    }
    
    


    private String getCompletionResponse(ApiKey apiKey, String userInput) {
        // Prepare the request body with necessary parameters
        JSONObject body = new JSONObject();
        body.put("prompt", userInput);
    
        // Calculate the maximum number of tokens for the response
        int maxTokens = calculateMaxTokens(apiKey.getMaxContextLength(), userInput.length());
        if (maxTokens <= 0) {
            throw new APIError(HttpStatus.EXPECTATION_FAILED, 
                    "Message is too long... Reduce input size.");
        }
        body.put("max_tokens", maxTokens);
    
        // Optional parameters can be set as needed
        body.put("temperature", 1.0);
        body.put("top_p", 1.0);
        body.put("n", 1);
        body.put("stream", false);
    
        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.getKeyValue());
    
        // Create a request entity
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
    
        // Initialize RestTemplate for the HTTP request
        RestTemplate restTemplate = new RestTemplate();
        String responseBody;
    
        try {
            // Send a POST request to the OpenAI completion API
            ResponseEntity<String> response = restTemplate.postForEntity(apiKey.getUri(), request, String.class);
            responseBody = response.getBody();
    
            // Handle HTTP status codes
            if (response.getStatusCode().is2xxSuccessful()) {
                // Extract and return the response text
                return extractTextFromChoices(responseBody);
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "API Error: " + responseBody);
            }
        } catch (HttpClientErrorException e) {
            throw new APIError(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
        


    private String extractTextFromChoices(String responseBody) {
        // Parse the JSON response
        JSONObject jsonResponse = new JSONObject(responseBody);
        JSONArray choices = jsonResponse.getJSONArray("choices");

        // Extract and concatenate text from all choices
        StringBuilder concatenatedText = new StringBuilder();
        for (int i = 0; i < choices.length(); i++) {
            JSONObject choice = choices.getJSONObject(i);
            String text = choice.getString("text");
            concatenatedText.append(text);
        }

        return concatenatedText.toString();
    }

    public String getApiId() {
        return "ChatGPT";
    }
}
