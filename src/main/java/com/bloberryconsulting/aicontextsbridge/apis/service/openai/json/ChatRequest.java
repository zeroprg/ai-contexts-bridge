package com.bloberryconsulting.aicontextsbridge.apis.service.openai.json;

import java.util.List;

public record ChatRequest(String model,
                          List<Message> messages,
                          double temperature) {}
