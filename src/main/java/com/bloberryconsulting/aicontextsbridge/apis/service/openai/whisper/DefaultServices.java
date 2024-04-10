package com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.bloberryconsulting.aicontextsbridge.apis.service.openai.tts.TextToSpeech;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.service.CryptoService;

@Service
@DependsOn("userService")
public class DefaultServices {
    private final Logger logger = LoggerFactory.getLogger(DefaultServices.class);
    public static final String SERVICE_IDENTIFIER = "DefaultAPIServices";
    private final CryptoService cryptoService;
    private final UserRepository userRepository;
    private final WhisperTranscribe whisperTranscribe;
    private final TextToSpeech textToSpeech;

    private String apiKey;
  

    public DefaultServices(CryptoService cryptoService, UserRepository userRepository, WhisperTranscribe whisperTranscribe, TextToSpeech textToSpeech) {        
        this.cryptoService = cryptoService;
        this.userRepository = userRepository;
        this.whisperTranscribe = whisperTranscribe;
        this.textToSpeech = textToSpeech;  
    }

    @PostConstruct
    public void init() {
        ApiKey apiKeyObject = userRepository.findApiKeysByModel(this.getApiId()).get();
        try {
            this.apiKey = decodeApiKey(apiKeyObject.getKeyValue());
            this.whisperTranscribe.setApiKey(this.apiKey);  
            this.textToSpeech.setApiKey(this.apiKey);
           
        } catch (Exception e) {
            logger.error("Error in decoding API key: {}", e.getLocalizedMessage());
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }   
    }

    public String getApiId() {
        return "whisper";
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
    


    // Logic to decode API by secret and strong decode algorithm
    private String decodeApiKey(String key) throws Exception {
        String decrypted = cryptoService.decrypt(key);
        return decrypted;
    }
}
