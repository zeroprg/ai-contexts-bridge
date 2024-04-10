package com.bloberryconsulting.aicontextsbridge.apis.service.openai.tts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.TTSRequest;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.Voice;
import com.bloberryconsulting.aicontextsbridge.apis.service.utilities.FileUtils;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.Objects;

@Service
public class TextToSpeech {
    private final Logger logger = LoggerFactory.getLogger(TextToSpeech.class);

    private static final String TTS_URL = "https://api.openai.com/v1/audio/speech";  
    @Value("${save.tts.audio}")
    private boolean saveTtsAudio;

    public final static String TTS_1 = "tts-1";
    public final static String TTS_1_HD = "tts-1-hd";

    private final FileUtils fileUtils;

    public TextToSpeech(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    private String apIKey = null;

    public void setApiKey(String key) {
        this.apIKey = key;
    }

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public byte[] generateMp3(TTSRequest ttsRequest) {
        String postBody = gson.toJson(ttsRequest);
        logger.info("postBody = {}", postBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_URL))
                .header("Authorization", "Bearer %s".formatted(apIKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postBody))
                .build();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();

              // Check if the request was successful
            if (response.statusCode() == 200) {
                // Write the audio bytes to an MP3 file
               if(saveTtsAudio){
                    Path fileName = fileUtils.writeSoundBytesToGivenFile(body);
                    logger.info("Saved {} to {}", fileName, fileName.getFileName());
                }
            } else {
                final String  result =  new String(body);
                logger.error("Error: {}", response.statusCode());
                logger.error("Error: {}",result);
                throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, result);
            }
            response.headers().map().forEach((k,v) -> logger.info("Header: {} = {}", k, v));
            return body;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public void playMp3UsingJLayer(String fileName) {
        var buffer = new BufferedInputStream(
                Objects.requireNonNull(getClass().getClassLoader()
                        .getResourceAsStream("audio/%s".formatted(fileName))));
        try {
            Player player = new Player(buffer);
            player.play();
        } catch (JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] createAndPlay(String text, Voice voice) {
        TTSRequest ttsRequest = new TTSRequest(TTS_1_HD,
                text.replaceAll("\\s+", " ").trim(), voice);
        byte[] bytes = generateMp3(ttsRequest);
        return bytes;
    }

}
