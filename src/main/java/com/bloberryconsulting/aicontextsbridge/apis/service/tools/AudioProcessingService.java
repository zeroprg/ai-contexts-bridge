package com.bloberryconsulting.aicontextsbridge.apis.service.tools;

import java.util.List;

import org.springframework.web.socket.WebSocketSession;

public interface AudioProcessingService {
    String getProcessorIdentifier(); // Return a unique identifier for each processor
    void processAudioChunks(WebSocketSession session, String languageCode, List<String> base64EncodedAudioChunks) throws Exception;
}
