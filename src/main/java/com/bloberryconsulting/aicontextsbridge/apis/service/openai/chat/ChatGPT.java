package com.bloberryconsulting.aicontextsbridge.apis.service.openai.chat;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bloberryconsulting.aicontextsbridge.apis.service.ApiService;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.ChatRequest;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.ChatResponse;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.Message;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.ModelList;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatGPT implements ApiService  {
    private static final String CHAT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODELS_URL = "https://api.openai.com/v1/models";

    public final static String GPT_35_TURBO = "gpt-3.5-turbo";
    public final static String GPT_4 = "gpt-4";
    public final static String GPT_4_TURBO = "gpt-4-1106-preview";

    public final static double DEFAULT_TEMPERATURE = 0.7;

    private final Logger logger = LoggerFactory.getLogger(ChatGPT.class);

    private final ChatGPTService chatGPTService;
    private final UserRepository userRepository;


    // Constructor for dependency injection
    public ChatGPT(ChatGPTService chatGPTService, UserRepository userRepository) {
        this.chatGPTService = chatGPTService;
        this.userRepository = userRepository;
    }



    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Role.class, new LowercaseEnumSerializer())
            .create();

    private final HttpClient client = HttpClient.newHttpClient();

    public List<ModelList.Model> listModels() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODELS_URL))
                .header("Authorization",
                        "Bearer %s".formatted(System.getenv("OPENAI_API_KEY")))
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            ModelList modelList = gson.fromJson(response.body(), ModelList.class);
            return modelList.data().stream()
                    .sorted(Comparator.comparing(ModelList.Model::created).reversed())
                    .toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Modified method to include contexts as a parameter
    public String getResponse(String prompt, String model, List<Context> contexts) {
        // Retrieve API key object using the model
        ApiKey apiKey = userRepository.findApiKeysByModel(model).orElseThrow(
            () -> new RuntimeException("No API key found for model: " + model)
        );

        // Use ChatGPTService for getting the response
        return chatGPTService.getResponse(apiKey, prompt, contexts);
    }

    public String getResponse(String prompt, String model) {
        return getResponse(prompt, model, DEFAULT_TEMPERATURE);
    }

    public String getResponse(String prompt, String model, double temperature) {
        ChatRequest chatRequest = createChatRequest(prompt, model, temperature);
        ChatResponse chatResponse = createChatResponse(chatRequest);
        return chatResponse.choices().get(0).message().content();
    }

    public ChatRequest createChatRequest(String prompt, String model, double temperature) {
        return new ChatRequest(model,
                List.of(new Message(Role.USER, prompt)),
                temperature);
    }

    public String getResponseToMessages(String model, Message... messages) {
        List<Message> messageList = List.of(messages);
        ChatRequest chatRequest = new ChatRequest(model, messageList, DEFAULT_TEMPERATURE);
        long startTime = System.nanoTime();
        ChatResponse chatResponse = createChatResponse(chatRequest);
        long endTime = System.nanoTime();
        logger.info("Elapsed time: {} ms", (endTime - startTime) / 1_000_000);
        logger.info(chatResponse.usage().toString());
        return extractStringResponse(chatResponse);
    }

    public String extractStringResponse(ChatResponse chatResponse) {
        return chatResponse.choices().get(0).message().content();
    }

    // Transmit the request to the OpenAI API and return the response
    public ChatResponse createChatResponse(ChatRequest chatRequest) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Authorization",
                        "Bearer %s".formatted(System.getenv("OPENAI_API_KEY")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(chatRequest)))
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Error: " + response.statusCode() + ": " + response.body());
            }
            return gson.fromJson(response.body(), ChatResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getResponse(ApiKey apiKey, String prompt, List<Context> contexts) {
        // Use ChatGPTService for getting the response
        return chatGPTService.getResponse(apiKey, prompt, contexts);
    }

    @Override
    public String getApiId() {
        return "ChatGPTBot";
    }

}