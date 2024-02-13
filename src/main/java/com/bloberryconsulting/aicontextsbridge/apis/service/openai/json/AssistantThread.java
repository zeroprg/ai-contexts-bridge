package com.bloberryconsulting.aicontextsbridge.apis.service.openai.json;

import java.util.Map;

public record AssistantThread(
        String id,
        String object,
        long createdAt,
        Map<String, Object> metadata
) {
}
