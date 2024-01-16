package com.bloberryconsulting.aicontextsbridge.apis.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.service.JsonUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChatGPTService implements ApiService {
    private final Logger logger = LoggerFactory.getLogger(ChatGPTService.class);
    private final static double RESPONSE_LENGTH_RATIO = 0.30; // 30% of the model's maximum token limit
    @Autowired
    private JsonUtils jsonUtils;

    private int calculateDesiredResponseLength(int maxModelTokens) {
        return (int) Math.round(maxModelTokens * RESPONSE_LENGTH_RATIO);
    }

    private int calculateMaxRequestLength(int maxModelTokens) {
        // Assuming you want to reserve a certain number of tokens for the response
        int reservedForResponse = calculateDesiredResponseLength(maxModelTokens);
        // The maximum request length would be the total token limit minus the reserved
        // tokens
        return maxModelTokens - reservedForResponse;
    }

    private int calculateMaxTokens(int maxModelTokens, int inputTokens) {
        int desiredResponseLength = calculateDesiredResponseLength(maxModelTokens);

        // Decide on a reasonable maximum limit for the response length
        // This is to prevent the model from generating an extremely long response
        int maxResponseTokens = maxModelTokens - inputTokens;

        // Ensure the maximum response tokens do not exceed the desired length
        return Math.min(maxResponseTokens, desiredResponseLength);
    }

    private String formPromptBasedOnContext(String prompt, List<Context> contexts) {
        StringBuilder collect = new StringBuilder();    
    
        for (Context context : contexts) {
            if (context.getConversationHistory() == null && context.getDocuments() == null) {
                continue;
            }
            String previousData = null;
            if (context != null) {
                String[] documents = context.getDocuments();
                if (documents != null && documents.length > 0) {
                    logger.info("Appending context to user input");
                    JSONArray jsonArray;
                    if (context.getConversationHistory() != null){
                        try {
                            jsonArray = jsonUtils.read(context.getConversationHistory());
                        } catch (IOException e) {
                            logger.error("Exception in maintainHistory: {}", e.getMessage());
                            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR,
                                    "Chat history serialization Error: " + e.getMessage());
                        }
                        // loop over all messages of JSONArray and find message that has role 'user' and
                        // which content has the documents values as string inside
                        // those documents which are not part of content will be added to the userInput
                        // in all other cases userInput will be returned as itself plus joined
                        // delimetered documents
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject message = jsonArray.getJSONObject(i);
                            if (message.getString("role").equals("user")) {
                                String content = message.getString("content");
                                for (String document : documents) {
                                    if (!content.contains(document)) {
                                        previousData = String.join("\n", documents) + "\n" + previousData;
                                    }
                                }
                            }
                        }
                    }
                    collect.append(((previousData == null) ? String.join("\n", documents) : previousData) + "\n");
                }

            }
        }
        return (!collect.isEmpty() ? collect.toString(): "") + prompt;
    }

    @Override
    public String getResponse(ApiKey apiKey, String prompt, List<Context> contexts) {
        logger.info("Entering getResponse method");
        if( contexts == null || contexts.size() == 0){
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Context not provided  ");
        }
        String userInput = formPromptBasedOnContext(prompt, contexts);

        try {
            if (apiKey.getUri().contains("/chat/completions")) {
                logger.info("Processing chat completion request");
                return getChatResponse(apiKey, userInput, contexts);
            } else {
                logger.info("Processing completion request");
                return getCompletionResponse(apiKey, userInput);
            }
        } catch (HttpClientErrorException e) {
            logger.error("HttpClientErrorException in getResponse: {}", e.getMessage());
            throw new APIError(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Exception in getResponse: {}", e.getMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        } finally {
            logger.info("Exiting getResponse method");
        }
    }

    private String getChatResponse(ApiKey apiKey, String userInput, List<Context> contexts ) {
        logger.info("Entering getChatResponse method");
        // Get the history from the first available context
        Context latestContext = contexts.stream().max(Comparator.comparing(Context::getLastUsed))
                                        .orElse(null);

        String modelName = apiKey.getModel();
        if (modelName == null || modelName.isEmpty()) {
            logger.error("Model name is null or empty");
            throw new APIError(HttpStatus.BAD_REQUEST, "Model name could not be extracted from the URI.");
        }

        JSONObject body = new JSONObject();
        body.put("model", modelName);

        JSONArray messages = maintainHistory(latestContext);
        if (latestContext.getAssistantRoleMessage() != null) {
            messages.put(new JSONObject().put("role", "system").put("content", latestContext.getAssistantRoleMessage()));
        }
        messages.put(new JSONObject().put("role", "user").put("content", userInput));

        messages = jsonUtils.manageTokenCountAndTrimHistory(messages,
                calculateMaxRequestLength(apiKey.getMaxContextLength()));

        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.getKeyValue());

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        String responseBody;

        try {
            logger.info("Sending POST request to OpenAI Chat API");
            logger.info("Request body: {}", body.toString());
            logger.debug("Request headers: {}", headers.toString());
            ResponseEntity<String> response = restTemplate.postForEntity(apiKey.getUri(), request, String.class);
            responseBody = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                String assistantResponse = extractTextFromChatResponse(responseBody);
                messages.put(new JSONObject().put("role", "assistant").put("content", assistantResponse));
                latestContext.setConversationHistory(jsonUtils.write(messages));
                return assistantResponse;
            } else {
                logger.error("Received non-2xx status code from OpenAI Chat API");
                throw new HttpClientErrorException(response.getStatusCode(), "API Error: " + responseBody);
            }
        } catch (IOException ex) {
            logger.error("IOException in messages: {}", ex.getMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Serialization failed: " + ex.getMessage());
        } catch (HttpClientErrorException e) {
            logger.error("HttpClientErrorException in getChatResponse: {}", e.getMessage());
            throw new APIError(e.getStatusCode(), e.getResponseBodyAsString());
        } finally {
            logger.info("Exiting getChatResponse method");
        }
    }

    private JSONArray maintainHistory(Context context) {
        try {

            if (context.getConversationHistory() == null) {
                // If conversationHistory is null, attempt to retrieve it from assistanceService

                JSONArray conversationHistory = new JSONArray();
                // store the history in the context
                context.setConversationHistory(jsonUtils.write(conversationHistory));
                return conversationHistory;
            } else {
                // Return the existing conversation history
                return jsonUtils.read(context.getConversationHistory());
            }
        } catch (Exception e) {
            logger.error("Exception in maintainHistory: {}", e.getMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, "Chat history serialization Error: " + e.getMessage());

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
            if (choice.has("message")) { // Check if the choice has a message
                JSONObject message = choice.getJSONObject("message");
                if (message.getString("role").equals("assistant")) {
                    concatenatedText.append(message.getString("content")).append("\n");
                }
            }
        }

        return concatenatedText.toString();
    }

    private String getCompletionResponse(ApiKey apiKey, String userInput) {
        logger.info("Entering getCompletionResponse method");

        // Log the input received
        logger.debug("Received userInput: {}", userInput);

        // Prepare the request body with necessary parameters
        JSONObject body = new JSONObject();
        body.put("prompt", userInput);
        logger.debug("Request body prepared with prompt");

        // Calculate the maximum number of tokens for the response
        int maxTokens = calculateMaxTokens(apiKey.getMaxContextLength(), userInput.length());
        logger.debug("Calculated maxTokens: {}", maxTokens);

        if (maxTokens <= 0) {
            logger.error("Max tokens calculation resulted in a non-positive value");
            throw new APIError(HttpStatus.EXPECTATION_FAILED,
                    "Message is too long... Reduce input size.");
        }
        body.put("max_tokens", maxTokens);

        // Optional parameters can be set as needed
        body.put("temperature", 1.0);
        body.put("top_p", 1.0);
        body.put("n", 1);
        body.put("stream", false);
        logger.debug("Optional parameters set");

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.getKeyValue());
        logger.debug("HTTP headers set");

        // Create a request entity
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        logger.debug("Request entity created");

        // Initialize RestTemplate for the HTTP request
        RestTemplate restTemplate = new RestTemplate();
        String responseBody;

        try {
            logger.info("Sending POST request to the OpenAI completion API");
            logger.info("Request body: {}", body.toString());
            logger.debug("Request headers: {}", headers.toString());

            // Send a POST request to the OpenAI completion API
            ResponseEntity<String> response = restTemplate.postForEntity(apiKey.getUri(), request, String.class);
            responseBody = response.getBody();
            logger.debug("Received response from API");

            // Handle HTTP status codes
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successful response received from API");
                // Extract and return the response text
                return extractTextFromChoices(responseBody);
            } else {
                logger.error("API returned non-success status code: {}", response.getStatusCode());
                throw new HttpClientErrorException(response.getStatusCode(), "API Error: " + responseBody);
            }
        } catch (HttpClientErrorException e) {
            logger.error("HttpClientErrorException caught: {}", e.getMessage());
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
