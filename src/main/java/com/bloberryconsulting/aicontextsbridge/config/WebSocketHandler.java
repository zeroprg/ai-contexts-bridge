package com.bloberryconsulting.aicontextsbridge.config;

import org.springframework.web.socket.WebSocketSession;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper.WhisperSpeechTotTextService;
import com.bloberryconsulting.aicontextsbridge.apis.service.tools.AudioProcessor;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;

import io.grpc.StatusRuntimeException;

import io.grpc.Status;

import java.io.EOFException;
import java.util.List;
import java.util.concurrent.Future;

@Deprecated
@Configuration
public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketHandler.class);
    private static final String STOP_STREAMING_MESSAGE = "STOP_STREAMING";
    private AudioProcessor audioProcessor;
    private static final String languageCode = "en-US";

    public WebSocketHandler(AudioProcessor audioProcessor) {
        this.audioProcessor = audioProcessor;
    }

    public void handleTextMessage_echotest(WebSocketSession session, TextMessage message) throws Exception {
        String clientMessage = message.getPayload();
        // logger.debug("Received message: " + clientMessage);
        logger.debug("session status is it open: {}", session.isOpen());
        session.sendMessage(new TextMessage(clientMessage));
    }

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String clientMessage = message.getPayload();
        logger.debug("message received in {} bytes", clientMessage.length());

        if (isStopStreamingMessage(clientMessage)) {
            // Process any remaining audio chunks before stopping the stream
            Future<?> future = audioProcessor.processRemainingChunks(session, languageCode,
                    WhisperSpeechTotTextService.SERVICE_IDENTIFIER);
            // Let APIError propagate if it occurs
            waitForFutureAndHandleExceptions(future); // This can throw InterruptedException or ExecutionException

        } else {
            JSONObject jsonData = new JSONObject(clientMessage);
            String base64Audio = jsonData.optString("audio");
            if (!base64Audio.isEmpty()) {
                Future<?> future = audioProcessor.processAudioChunks(session, languageCode, List.of(base64Audio),
                WhisperSpeechTotTextService.SERVICE_IDENTIFIER);
                // Let APIError propagate if it occurs
                waitForFutureAndHandleExceptions(future);
            }
        }

        session.sendMessage(new TextMessage("test"));
    }

    public void waitForFutureAndHandleExceptions(Future<?> future) {
        try {
            future.get();
        } catch (Exception e) {

            Throwable cause = e.getCause();
            if (cause instanceof StatusRuntimeException) {
                StatusRuntimeException statusException = (StatusRuntimeException) cause;
                // Handle the PERMISSION_DENIED exception
                if (statusException.getStatus().getCode() == Status.Code.PERMISSION_DENIED) {
                    throw new APIError(HttpStatus.UNAUTHORIZED,
                            "Permission denied. Please check your API key and try again later. ");
                }
            }
            throw new APIError(HttpStatus.NON_AUTHORITATIVE_INFORMATION, e.getLocalizedMessage());
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
        logger.debug("New WebSocket connection established. Session ID: " +  session.getId());

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (exception instanceof EOFException) {
            // Specific handling for EOFException
            logger.error("WebSocket connection abruptly closed for {}", session.getId());
            session.close();
        } else {
            // General error handling
            logger.error("Transport error in WebSocket session", exception);
        }

        // Cleanup resources related to the session, if any
        // ...
    }

}
