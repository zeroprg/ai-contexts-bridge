package com.bloberryconsulting.aicontextsbridge.config;

import org.springframework.web.socket.WebSocketSession;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.bloberryconsulting.aicontextsbridge.apis.service.GoogleStreamSpeechToTextService;

import java.util.Base64;

@Configuration
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketHandler.class);
    private static final String STOP_STREAMING_MESSAGE = "STOP_STREAMING";
    private GoogleStreamSpeechToTextService googleStreamSpeechToTextService;

    public WebSocketHandler(GoogleStreamSpeechToTextService googleStreamSpeechToTextService) {
        this.googleStreamSpeechToTextService = googleStreamSpeechToTextService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientMessage = message.getPayload();
        logger.debug("Received message: " + clientMessage);

        if (isStopStreamingMessage(clientMessage)) {
            googleStreamSpeechToTextService.stopStreaming(session.getId());
        } else {
            JSONObject jsonData = new JSONObject(clientMessage);
            String base64Audio = jsonData.optString("audio");
            if (!base64Audio.isEmpty()) {
                // Decode Base64 string to bytes
                byte[] audioBytes = Base64.getDecoder().decode(base64Audio);
                googleStreamSpeechToTextService.streamAudio(audioBytes);
            }
        }
    }

    private boolean isStopStreamingMessage(String clientMessage) {
        if (clientMessage != null) {
            JSONObject jsonData = new JSONObject(clientMessage);
            return jsonData.optBoolean(STOP_STREAMING_MESSAGE, false);
        }
        return false;
    }

@Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.debug("New WebSocket connection established. Session ID: " + session.getId());
        // Initiate streaming when the connection is established
        googleStreamSpeechToTextService.startStreaming(session.getId());
    }
}
