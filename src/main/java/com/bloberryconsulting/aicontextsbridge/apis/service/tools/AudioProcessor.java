package com.bloberryconsulting.aicontextsbridge.apis.service.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.socket.WebSocketSession;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;

public class AudioProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AudioProcessor.class);

    private final Map<String, AudioProcessingService> audioProcessors;
    private final Map<String, List<String>> sessionAudioChunks;
    private final ExecutorService executorService;
    private final int batchSize = 5; // Hardcoded batch size

    public AudioProcessor(List<AudioProcessingService> audioProcessingServices) {
        this.audioProcessors = audioProcessingServices.stream()
            .collect(Collectors.toMap(
                AudioProcessingService::getProcessorIdentifier,
                Function.identity()
            ));
        this.sessionAudioChunks = new ConcurrentHashMap<>(); // Thread-safe map
        this.executorService = Executors.newFixedThreadPool(1);
    }



    private Future<?> processAudio(WebSocketSession session, String languageCode, List<String> audioChunks, String selectedProcessor) {
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

        executorService.submit(() -> {
            try {
                AudioProcessingService processor = audioProcessors.get(selectedProcessor);
                if (processor != null) {
                    try {
                        processor.processAudioChunks(session, languageCode, audioChunks);                  
                    } catch (Exception e) {
                        logger.error("Error processing audio: " + e.getLocalizedMessage());
                        completableFuture.completeExceptionally(e);
                        return;
                    }
                } else {
                    String errorMsg = "Selected processor not found: " + selectedProcessor;
                    logger.error(errorMsg);
                    completableFuture.completeExceptionally(new APIError(HttpStatus.NON_AUTHORITATIVE_INFORMATION, errorMsg));
                    return;
                }
            } catch (Exception e) {
                logger.error("Error processing audio: " + e.getLocalizedMessage());
                completableFuture.completeExceptionally(new APIError(HttpStatus.NON_AUTHORITATIVE_INFORMATION, e.getLocalizedMessage()));
                return;
            }
        });

        return completableFuture;
    }

    public Future<?> processAudioChunks(WebSocketSession session, String languageCode, List<String> base64EncodedAudioChunks, String selectedProcessor) {
        return processAudio(session, languageCode, base64EncodedAudioChunks, selectedProcessor);
    }

    private Future<?> processBatch(WebSocketSession session, String languageCode, String selectedProcessor, String sessionId) {
        List<String> chunksToProcess = new ArrayList<>(sessionAudioChunks.get(sessionId));
        sessionAudioChunks.get(sessionId).clear();
        return processAudio(session, languageCode, chunksToProcess, selectedProcessor);
    }



    // Additional method to process remaining chunks for a session
    public Future<?> processRemainingChunks(WebSocketSession session, String languageCode, String selectedProcessor) {
        String sessionId = session.getId();
        if (sessionAudioChunks.containsKey(sessionId) && !sessionAudioChunks.get(sessionId).isEmpty()) {
            return processBatch(session, languageCode, selectedProcessor, sessionId);
        }
        String errorMsg = String.format("Selected WebSocket session %s not found: ", sessionId);        
        logger.error(errorMsg);
        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
        completableFuture.completeExceptionally(new APIError(HttpStatus.NON_AUTHORITATIVE_INFORMATION, errorMsg));
        return  completableFuture;
    }
}
