package com.bloberryconsulting.aicontextsbridge.apis.service;

import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.protobuf.ByteString;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;

import java.net.URL;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONObject;

@Service
@Profile("notActiveProfile")
public class GoogleSpeechToTextService implements ApiService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechToTextService.class);
   
    @Override
    public String getResponse(ApiKey apiKey, String data, List<Context> contextHistory) {
        try {
            JSONObject jsonData = new JSONObject(data);

            if (jsonData.has("audio")) {
                
                String base64Audio = jsonData.getString("audio");

                return getResponseFromStream(apiKey, base64Audio, contextHistory, "en-US"); // Assuming default language
            } else if (jsonData.has("fileName")) {
                String fileUrl = jsonData.getString("fileName");
                return getResponseFromFile(apiKey, fileUrl, contextHistory, "en-US"); // Assuming default language
            } else {
                throw new IllegalArgumentException("Invalid data format");
            }
        } catch (Exception e) {
            logger.error("Error in getResponse: {}", data, e);
            return "Error processing request: " + e.getMessage();
        }
    }

    public String getResponseFromFile(ApiKey apiKey, String fileUrl, List<Context> contextHistory,
            String languageCode) {
        try {
            Path tempFile = Files.createTempFile("speech_", ".tmp");
            try (InputStream in = new URL(fileUrl).openStream()) {
                Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] data = Files.readAllBytes(tempFile);
            ByteString audioBytes = ByteString.copyFrom(data);
            logger.debug("Processing audio file from URL: {}", fileUrl);

            return transcribeAudio(apiKey, audioBytes, contextHistory, languageCode);
        } catch (IOException e) {
            logger.error("Error processing audio file from URL: {}", fileUrl, e);
            return "Error processing audio file: " + e.getMessage();
        }
    }

    public String getResponseFromStream(ApiKey apiKey, String base64EncodedAudio, List<Context> contextHistory,
            String languageCode) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedAudio);
            ByteString audioBytes = ByteString.copyFrom(decodedBytes);
            logger.debug("Processing audio from Base64 encoded string");
            return transcribeAudio(apiKey, audioBytes, contextHistory, languageCode);
        } catch (IllegalArgumentException e) {
            logger.error("Error decoding Base64 audio", e);
            return "Error decoding Base64 audio: " + e.getMessage();
        }
    }

    private String transcribeAudio(ApiKey apiKey, ByteString audioBytes, List<Context> contextHistory,
            String languageCode) {
        try (SpeechClient speechClient = SpeechClient.create()) {
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode(languageCode)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            logger.debug("Starting transcription process");
            List<SpeechRecognitionResult> results = speechClient.recognize(config, audio).getResultsList();

            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : results) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.append(alternative.getTranscript());
            }
            logger.debug("Transcription completed successfully");
            return transcription.toString();
        } catch (Exception e) {
            logger.error("Error during transcription process", e);
            return "Error processing audio: " + e.getMessage();
        }
    }

    @Override
    public String getApiId() {
        return "Google Speech-to-Text API Key";
    }
}
