package com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.bloberryconsulting.aicontextsbridge.apis.service.ApiService;
import com.bloberryconsulting.aicontextsbridge.apis.service.tools.AudioProcessingService;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.service.CryptoService;

@Deprecated
@Service
@DependsOn("userService")
public class WhisperSpeechTotTextService implements ApiService, AudioProcessingService {
    private final Logger logger = LoggerFactory.getLogger(WhisperSpeechTotTextService.class);
    public static final String SERVICE_IDENTIFIER = "WhisperStreamSpeechToTextService";
    private final CryptoService cryptoService;
    private final UserRepository userRepository;
    private final WhisperTranscribe whisperTranscribe;

    private String apiKey;
    private WebSocketSession session;
  

    public WhisperSpeechTotTextService(CryptoService cryptoService, UserRepository userRepository, WhisperTranscribe whisperTranscribe) {        
        this.cryptoService = cryptoService;
        this.userRepository = userRepository;
        this.whisperTranscribe = whisperTranscribe;     
    }

    @PostConstruct
    public void init() {
        ApiKey apiKeyObject = userRepository.findApiKeysByModel(this.getApiId()).get();
        try {
            this.apiKey = decodeApiKey(apiKeyObject.getKeyValue());
            this.whisperTranscribe.setApiKey(this.apiKey);  
           
        } catch (Exception e) {
            logger.error("Error in decoding API key: {}", e.getLocalizedMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }   
    }

    @Override
    public String getResponse(ApiKey apiKey, String base64EncodedAudioChunk, List<Context>  contextHistory) {
        // Implementation for ApiService
    
        String transcript = "";
        try {
            transcript = processBufferedAudio( base64EncodedAudioChunk);
        } catch (IOException | InterruptedException e) {            
            logger.error("Error in processing audio chunk: {}", e.getLocalizedMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
        return transcript;
    }

    @Override
    public String getApiId() {
        return "whisper";
    }

    @Override
    public String getProcessorIdentifier() {
        return SERVICE_IDENTIFIER;
    }

 

    private Path convertBase64AudioToFile(String base64Audio, String fileExtension) throws IOException {
        // Check for and remove the data URL prefix if present
        String base64Data = base64Audio.startsWith("data:") ? 
                            base64Audio.substring(base64Audio.indexOf(",") + 1) : 
                            base64Audio;
    
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);
        Path tempFile = Files.createTempFile("audio", "." + fileExtension); // e.g., ".wav"
        Files.write(tempFile, audioBytes);
        return tempFile;
    }
    

    

    private String processBufferedAudio(String base64EncodedAudioChunk) throws IOException, InterruptedException {
        Path fileNamePath = null; // Initialize with a default value
        try {
            fileNamePath = convertBase64AudioToFile(base64EncodedAudioChunk, "ogg");
            this.logger.debug("Processing audio file: {}", fileNamePath);
          
            final String transription = whisperTranscribe.transcribe(fileNamePath);
            return transription;
        } finally {           
            Files.deleteIfExists(fileNamePath); // Ensure file deletion after transcription            
        }
    }
    
    
    
    

    // Implementation for AudioProcessingService to process audio chunks by through the websocket
    @Override
    public void processAudioChunks(WebSocketSession session, String languageCode, List<String> base64EncodedAudioChunks) throws Exception {
        this.session = session;
        CompletableFuture.runAsync(() -> {
            for (String base64Audio : base64EncodedAudioChunks) {
                try {
                    String transcription = processBufferedAudio(base64Audio);
                    sendTranscription(transcription);
                } catch (Exception e) {
                    // Handle exceptions and potentially send error messages back via WebSocket
                    Thread.currentThread().interrupt();
                    throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
                    
                }
            }
        });
    }


    private void sendTranscription(String transcription) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(transcription));
            }
        } catch (IOException e) {
            // Handle WebSocket communication errors
        }
    }

    // Logic to decode API by secret and strong decode algorithm
    private String decodeApiKey(String key) throws Exception {
        String decrypted = cryptoService.decrypt(key);
        return decrypted;
    }

}
