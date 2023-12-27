package com.bloberryconsulting.aicontextsbridge.apis.service;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class GoogleSpeechToTextService implements ApiService{


    public String getResponse(ApiKey apiKey, String audioFilePath) {
        try (SpeechClient speechClient = SpeechClient.create()) {
            // The path to the audio file to transcribe
            byte[] data = Files.readAllBytes(Paths.get(audioFilePath));
            ByteString audioBytes = ByteString.copyFrom(data);

            // Configure request with local raw PCM audio
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setLanguageCode("en-US")
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioBytes)
                    .build();

            // Use blocking call to get audio transcript
            List<SpeechRecognitionResult> results = speechClient.recognize(config, audio).getResultsList();

            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : results) {
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                transcription.append(alternative.getTranscript());
            }
            return transcription.toString();
        } catch (Exception e) {
            // Handle exceptions
            return "Error processing audio file: " + e.getMessage();
        }
    }

    @Override
    public String getApiId() {
        return "Google Speech-to-Text API Key";
    }
}
