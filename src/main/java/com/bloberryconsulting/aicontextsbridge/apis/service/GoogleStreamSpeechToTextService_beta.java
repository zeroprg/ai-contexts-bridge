package com.bloberryconsulting.aicontextsbridge.apis.service;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.google.api.gax.rpc.ApiStreamObserver;
import com.google.api.gax.rpc.BidiStreamingCallable;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;


@Service
@Profile("notActiveProfile")
public class GoogleStreamSpeechToTextService_beta extends AbstractGoogleAPIs implements ApiService {
    private final Logger logger = LoggerFactory.getLogger(GoogleStreamSpeechToTextService_beta.class);
    
    @Value("${google.credentials.file.path}")
    private String apiKeyFilePath;
    private SpeechSettings speechSettings;

    private final Map<String, Boolean> activeSessions = new ConcurrentHashMap<>();

    private final BlockingQueue<ByteString> requestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<StreamingRecognizeResponse> responseQueue = new LinkedBlockingQueue<>();
    //private final ExecutorService executorService = Executors.newCachedThreadPool();



    public void startStreaming(String sessionId) throws Exception {
        // Check if streaming is already active for this session
        if (activeSessions.putIfAbsent(sessionId, true) != null) {
            logger.info("Streaming is already active for session {}", sessionId);
            return; // Exit if streaming is already started for this session
        }
        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
            BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable = speechClient.streamingRecognizeCallable();

            // Start request handling thread
            Thread requestHandlingThread = new Thread(() -> handleRequests(callable));
            requestHandlingThread.start();

            // Start response handling thread
            Thread responseHandlingThread = new Thread(() -> handleResponses(sessionId));
            responseHandlingThread.start();

            // Wait for threads to complete
            //requestHandlingThread.join();
            //responseHandlingThread.join();
        }
    }

    public void streamAudio(byte[] audioBytes) {
        if (audioBytes != null && audioBytes.length > 0) {
            requestQueue.add(ByteString.copyFrom(audioBytes));
        } else {
            logger.warn("Received empty audio bytes for streaming");
        }
    }
    
    public void stopStreaming(String sessionId) {
        // Add end-of-stream marker to the request queue
        requestQueue.add(ByteString.EMPTY);
        activeSessions.remove(sessionId); // Remove the session from active sessions

        // Optionally, perform additional actions such as cleaning up resources 
        // or notifying other components that the streaming session has ended.
        logger.info("Streaming stopped for session {}", sessionId);
    }
    

    private void handleRequests(BidiStreamingCallable<StreamingRecognizeRequest, StreamingRecognizeResponse> callable) {
        ApiStreamObserver<StreamingRecognizeRequest> requestObserver = 
            callable.bidiStreamingCall(new ApiStreamObserver<StreamingRecognizeResponse>() {
                @Override
                public void onNext(StreamingRecognizeResponse response) {
                    responseQueue.add(response);
                }

                @Override
                public void onError(Throwable throwable) {
                    // Handle error
                }

                @Override
                public void onCompleted() {
                    // Handle completion
                }
            });
    
        try {
            while (true) {
                ByteString audioChunk = requestQueue.take();
                if (audioChunk.isEmpty()) { // End-of-stream marker
                    break;
                }
                StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(audioChunk)
                    .build();
                requestObserver.onNext(request);
            }
        } catch (InterruptedException e) {
            logger.info("Request handling thread interrupted", e);
            // Optionally handle interruption
        } finally {
            requestObserver.onCompleted();
        }
    }
    
    private void handleResponses(String sessionId) {
        try {
            while (true) {
                StreamingRecognizeResponse response = responseQueue.take();
                if (isEndOfStreamMarker(response)) {
                    break; // Exit loop if end-of-stream marker is detected
                }
                processResponse(sessionId, response);
            }
        } catch (InterruptedException e) {
            logger.info("Response handling thread interrupted", e);
            // Optionally handle interruption
        }
    }
    
    private boolean isEndOfStreamMarker(StreamingRecognizeResponse response) {
        // Implement logic to determine if this response is the end-of-stream marker
        // This could be based on a flag in your application logic
        return false; // Placeholder implementation
    }
    
    private void processResponse(String sessionId, StreamingRecognizeResponse response) {
        // Process each response
        // Example: Extracting and logging the transcript
        response.getResultsList().stream()
            .filter(StreamingRecognitionResult::getIsFinal)
            .flatMap(result -> result.getAlternativesList().stream())
            .forEach(alternative -> logger.info("Transcript: {}", alternative.getTranscript()));
    }

    @Override
    public String getResponse(ApiKey apiKey, String message, List<Context> contextHistory) {
      return null;    }

    @Override
    public String getApiId() {
        return "Google Speech-to-Text Stream API Key";
    }
    
    // Additional methods and logic as needed...
}    