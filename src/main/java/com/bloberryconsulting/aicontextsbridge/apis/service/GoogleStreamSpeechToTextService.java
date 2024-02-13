package com.bloberryconsulting.aicontextsbridge.apis.service;
import com.bloberryconsulting.aicontextsbridge.apis.service.tools.AudioProcessingService;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

// example of calling: " infiniteStreamingRecognize(options.langCode, base64EncodedAudioChunks);"

@Service
@Profile("notActiveProfile")
public class GoogleStreamSpeechToTextService extends AbstractGoogleAPIs implements ApiService, AudioProcessingService{
    private final Logger logger = LoggerFactory.getLogger(GoogleStreamSpeechToTextService.class);

    public static final String SERVICE_IDENTIFIER = "GoogleStreamSpeechToTextService";
    private static final int STREAMING_LIMIT = 290000; // ~5 minutes

    private static final String RED = "\033[0;31m";
    private static final String GREEN = "\033[0;32m";
    private static final String YELLOW = "\033[0;33m";


    // Creating shared object
  private static volatile BlockingQueue<byte[]> sharedQueue = new LinkedBlockingQueue<byte[]>();
  private static TargetDataLine targetDataLine;
  private static int BYTES_PER_BUFFER = 6400; // buffer size in bytes

    private static ArrayList<ByteString> audioInput = new ArrayList<>();
    
    private static ArrayList<ByteString> lastAudioInput = new ArrayList<>();
    private static int restartCounter = 0;
    private static int resultEndTimeInMS = 0;
    private static int isFinalEndTime = 0;
    private static int finalRequestEndTime = 0;
    private static boolean newStream = true;
    private static double bridgingOffset = 0;
    private static boolean lastTranscriptWasFinal = false;
    private static StreamController referenceToStreamController;

    private WebSocketSession session;
   



    public static String convertMillisToDate(double milliSeconds) {
        long millis = (long) milliSeconds;
        DecimalFormat format = new DecimalFormat();
        format.setMinimumIntegerDigits(2);
        return String.format(
                "%s:%s /",
                format.format(TimeUnit.MILLISECONDS.toMinutes(millis)),
                format.format(
                        TimeUnit.MILLISECONDS.toSeconds(millis)
                                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))));
    }

    private CompletableFuture<Void> future = new CompletableFuture<>();
    public CompletableFuture<Void> getFuture() {
        return future;
    }
    


    public void infiniteStreamingRecognize(String languageCode, List<String> base64EncodedAudioChunks) throws Exception  {
        ResponseObserver<StreamingRecognizeResponse> responseObserver = setupResponseObserver();
  
        
        try(SpeechClient client = SpeechClient.create(speechSettings)){            
            
            ClientStream<StreamingRecognizeRequest> clientStream = setupClientStream(client, languageCode, responseObserver);
            long startTime = System.currentTimeMillis();
            getFuture().get();
            if(clientStream != null && clientStream.isSendReady()){
            for (String base64EncodedAudioChunk : base64EncodedAudioChunks) {
                processAudioChunk(base64EncodedAudioChunk, clientStream);
                manageStreamingSession(client, clientStream, responseObserver, startTime, languageCode );
            }
        }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }    
        
    }
    

    private static ClientStream<StreamingRecognizeRequest> setupClientStream(SpeechClient client, String languageCode, ResponseObserver<StreamingRecognizeResponse> responseObserver) {
        ClientStream<StreamingRecognizeRequest> clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);
        sendInitialRequest(clientStream, languageCode);
        return clientStream;
    }

    private static void sendInitialRequest(ClientStream<StreamingRecognizeRequest> clientStream, String languageCode) {
        RecognitionConfig recognitionConfig = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setLanguageCode(languageCode)
                .setSampleRateHertz(16000)
                .build();

        StreamingRecognitionConfig streamingRecognitionConfig = StreamingRecognitionConfig.newBuilder()
                .setConfig(recognitionConfig)
                .setInterimResults(true)
                .build();

        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(streamingRecognitionConfig)
                .build(); // The first request in a streaming call has to be a config

        clientStream.send(request);
    }

    private  ResponseObserver<StreamingRecognizeResponse> setupResponseObserver() {
        return new ResponseObserver<StreamingRecognizeResponse>() {
            ArrayList<StreamingRecognizeResponse> responses = new ArrayList<>();

            public void onStart(StreamController controller) {
                referenceToStreamController = controller;
            }

            public void onResponse(StreamingRecognizeResponse response) {
                processResponse(response);
            }

            public void onComplete() {
            }

            public void onError(Throwable t) {
                logger.error("Error in response observer: " + t.getMessage());                
                future.completeExceptionally(new APIError(HttpStatus.INTERNAL_SERVER_ERROR, t.getLocalizedMessage()));       
            }

            private void processResponse(StreamingRecognizeResponse response) {
                responses.add(response);
                StreamingRecognitionResult result = response.getResultsList().get(0);
                Duration resultEndTime = result.getResultEndTime();
                resultEndTimeInMS =
                    (int)
                        ((resultEndTime.getSeconds() * 1000) + (resultEndTime.getNanos() / 1000000));
                double correctedTime =
                    resultEndTimeInMS - bridgingOffset + (STREAMING_LIMIT * restartCounter);
  
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                if (result.getIsFinal()) {
                  System.out.print(GREEN);
                  System.out.print("\033[2K\r");
                  System.out.printf(
                      "%s: %s [confidence: %.2f]\n",
                      convertMillisToDate(correctedTime),
                      alternative.getTranscript(),
                      alternative.getConfidence());

                      onTranscriptionResult(session, alternative.getTranscript());   
                  isFinalEndTime = resultEndTimeInMS;
                  lastTranscriptWasFinal = true;
                } else {
                  System.out.print(RED);
                  System.out.print("\033[2K\r");
                  System.out.printf(
                      "%s: %s", convertMillisToDate(correctedTime), alternative.getTranscript());
                  lastTranscriptWasFinal = false;
                }
              }

            private void onTranscriptionResult(WebSocketSession session, String transcript) {
                try {
                    session.sendMessage (new TextMessage(transcript));
                } catch (IOException e) {    
                    throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage() + "for: "+transcript);
                }           
            }
        };
    }

    // Process a single Base64 encoded audio chunk
    public void processAudioChunk(String base64EncodedAudio, ClientStream<StreamingRecognizeRequest> clientStream) {
        byte[] audioBytes = Base64.getDecoder().decode(base64EncodedAudio);
        ByteString audioByteString = ByteString.copyFrom(audioBytes);
        audioInput.add(audioByteString);

        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().setAudioContent(audioByteString).build();
        clientStream.send(request);
    }

    private void manageStreamingSession(SpeechClient client, ClientStream<StreamingRecognizeRequest> clientStream, ResponseObserver<StreamingRecognizeResponse> responseObserver, long startTime, String languageCode) throws Exception {
        long estimatedTime = System.currentTimeMillis() - startTime;
        
        if (estimatedTime >= STREAMING_LIMIT) {
          handleStreamingLimitReached(client, clientStream, responseObserver, languageCode);
          startTime = System.currentTimeMillis();
        } else {
          sendAudioData(clientStream);
        }
    }
    
    private  void sendAudioData(ClientStream<StreamingRecognizeRequest> clientStream) throws InterruptedException {
        ByteString tempByteString = ByteString.copyFrom(sharedQueue.take());
        StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().setAudioContent(tempByteString).build();
        audioInput.add(tempByteString);
        clientStream.send(request);
    }

    private void handleStreamingLimitReached(SpeechClient client, ClientStream<StreamingRecognizeRequest> clientStream, ResponseObserver<StreamingRecognizeResponse> responseObserver, String languageCode) {
            clientStream.closeSend();
            referenceToStreamController.cancel(); // remove Observer

            if (resultEndTimeInMS > 0) {
              finalRequestEndTime = isFinalEndTime;
            }
            resultEndTimeInMS = 0;
            
            lastAudioInput = new ArrayList<>(audioInput);
            audioInput.clear();
            
            clientStream = client.streamingRecognizeCallable().splitCall(responseObserver);
            sendInitialRequest(clientStream, languageCode);
            
            System.out.println(YELLOW);
            System.out.printf("%d: RESTARTING REQUEST\n", ++restartCounter * STREAMING_LIMIT);
    }

    


    // New method for getting a response from a base64 encoded audio
    public String getResponse(String languageCode, String base64EncodedAudio) {
        // Implementation of API call logic using base64EncodedAudio
        return null; // Placeholder for actual implementation
    }

    @Override
    public String getResponse(ApiKey apiKey, String message, List<Context> contextHistory) {
        return null; 
    }

    @Override
    public String getApiId() {
        return  SERVICE_IDENTIFIER;
    }

    @Override
    public void processAudioChunks(WebSocketSession session, String languageCode, List<String> base64EncodedAudioChunks) throws Exception {
        this.session = session;
        infiniteStreamingRecognize(languageCode, base64EncodedAudioChunks);

    }

    @Override
    public String getProcessorIdentifier() {
       return  SERVICE_IDENTIFIER;
    }
}
