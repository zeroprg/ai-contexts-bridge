package com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper;

import com.bloberryconsulting.aicontextsbridge.apis.service.openai.chat.ChatGPT;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.chat.Role;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.Message;
import com.bloberryconsulting.aicontextsbridge.apis.service.utilities.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

// https://platform.openai.com/docs/tutorials/meeting-minutes
// Transcribe and analyze meeting minutes tutorial
@Service
public class WhisperTutorial {

    private final WhisperTranscribe whisperTranscribe;

    private final ChatGPT chatGPT = new ChatGPT(null,null);

    private final FileUtils fileUtils;

    public WhisperTutorial(FileUtils fileUtils , WhisperTranscribe whisperTranscribe) { 
        this.fileUtils = fileUtils;
        this.whisperTranscribe = whisperTranscribe; 
    }   

    public void processMeetingMinutes() throws IOException {
        // Transcribe audio, or load transcription if it already exists
        String transcription = getTranscription("EarningsCall");

        Map<String, String> promptMap = Map.ofEntries(
                Map.entry("summarize", TutorialPrompts.SUMMARIZE_PROMPT),
                Map.entry("key_points", TutorialPrompts.KEY_POINTS_PROMPT),
                Map.entry("action_items", TutorialPrompts.ACTION_ITEMS_PROMPT),
                Map.entry("sentiment", TutorialPrompts.SENTIMENT_PROMPT)
        );

        // Call GPT-4 to get the responses to each prompt
        long startTime = System.nanoTime();
        ConcurrentMap<String, String> responseMap = promptMap.entrySet().parallelStream()
                .peek(e -> System.out.println("Processing " + e.getKey()))
                .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                e -> getResponse(e.getValue(), transcription)
                        )
                );
        long endTime = System.nanoTime();
        System.out.printf("Elapsed time: %.3f seconds%n", (endTime - startTime) / 1e9);

        responseMap.forEach((name, response) ->
                {
                    try {
                        fileUtils.writeTextToFile(response, name + ".txt");
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                });
        fileUtils.writeWordDocument(responseMap);
    }

    public String getResponse(String prompt, String transcription) {
        return chatGPT.getResponseToMessages(ChatGPT.GPT_4,
                new Message(Role.SYSTEM, prompt),
                new Message(Role.USER, transcription));
    }

    @SuppressWarnings("SameParameterValue")
    public String getTranscription(String fileName) throws IOException {
        Path transcriptionFilePath = Paths.get(FileUtils.TEXT_RESOURCES_PATH, fileName + ".txt");
        Path audioFilePath = Paths.get(FileUtils.AUDIO_RESOURCES_PATH, fileName + ".wav");

        if (Files.exists(transcriptionFilePath)) {
            try {
                return Files.readString(transcriptionFilePath);
            } catch (IOException e) {
                System.err.println("Error reading transcription file: " + e.getMessage());
            }
        } else {
            return whisperTranscribe.transcribe(audioFilePath);
        }
        return "";
    }
}
