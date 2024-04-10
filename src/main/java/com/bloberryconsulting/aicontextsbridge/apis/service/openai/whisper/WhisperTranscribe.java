package com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.apis.service.utilities.FileUtils;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// See docs at https://platform.openai.com/docs/api-reference/audio/createTranscription

// response_format: json (default), text, srt, verbose_json, vtt
//      "text" is used here, as it returns the transcript directly
// language: ISO-639-1 code (optional)
//
// Rather than use multipart form data, add the file as a binary body directly
// Optional "prompt" used to give standard word spellings whisper might miss
//      If there are multiple chunks, the prompt for subsequent chunks should be the
//      transcription of the previous one (244 tokens max)

// file must be mp3, mp4, mpeg, mpga, m4a, wav, or webm
// NOTE: only wav files are supported here (mp3 apparently is proprietary)

// max size is 25MB; otherwise need to break the file into chunks
// See the WavFileSplitter class for that

@Service
public class WhisperTranscribe {
    private final Logger logger = LoggerFactory.getLogger(WhisperTranscribe.class);
    private final static String URL = "https://api.openai.com/v1/audio/transcriptions";
    public final static int MAX_ALLOWED_SIZE = 25 * 1024 * 1024;
    public final static int MAX_CHUNK_SIZE_BYTES = 20 * 1024 * 1024;

    private final FileUtils fileUtils;

    private String apIKey = null;
    private String terminologyPrompt;

    public WhisperTranscribe(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    public void setApiKey(String key) {
        this.apIKey = key;
    }

    public String getApiKey() {
        return apIKey;
    }

    // Only model available as of Fall 2023 is whisper-1
    private final static String MODEL = "whisper-1";

    // Prompt for the first chunk
    public String getTerminologyPrompt() {
        return terminologyPrompt;
    }

    public void setTerminologyPrompt(String prompt) {
        this.terminologyPrompt = prompt;

    }

    private static final String TERMINOLOGY_GUIDE_FILES = "./terminology-guide/";

    private String transcribeChunk(String prompt, File chunkFile) {
        System.out.printf("Transcribing %s%n", chunkFile.getName());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Authorization", "Bearer %s".formatted(apIKey));

            HttpEntity entity = MultipartEntityBuilder.create()
                    .setContentType(ContentType.MULTIPART_FORM_DATA)
                    .addPart("file", new FileBody(chunkFile, ContentType.DEFAULT_BINARY))
                    .addPart("model", new StringBody(MODEL, ContentType.DEFAULT_TEXT))
                    .addPart("response_format", new StringBody("text", ContentType.DEFAULT_TEXT))
                    .addPart("prompt", new StringBody(prompt, ContentType.DEFAULT_TEXT))
                    .build();
            httpPost.setEntity(entity);

            return client.execute(httpPost, response -> {
                logger.debug("Status: " + new StatusLine(response));
                final String transript = EntityUtils.toString(response.getEntity());
                checkForError(transript);
                return transript;
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void checkForError(String transcription) {
        if (transcription != null && transcription.contains("error:")) {
            JSONObject jsonResponse = new JSONObject(transcription);
            if (jsonResponse.has("error")) {
                JSONObject errorObject = jsonResponse.getJSONObject("error");
                String errorMessage = errorObject.getString("message");
                throw new RuntimeException("Error transcribing chunk: " + errorMessage);
            }
        }
    }

    public String transcribe(Path filePath) throws IOException {
        logger.debug("Transcribing " + filePath);
        File file = filePath.toFile();

        // Collect the transcriptions of each chunk
        List<String> transcriptions = new ArrayList<>();

        // First prompt is the word list
        // TODO: get the word list from the terminology guide make ConcurrentHashMap to
        // stay away from concurent modification
        String prompt = getTerminologyPrompt()==null ?"":getTerminologyPrompt();

        if (file.length() <= MAX_ALLOWED_SIZE) {
            String transcription = transcribeChunk(prompt, file);
            transcriptions = List.of(transcription);
        } else {
            var splitter = new WavFileSplitter();
            List<File> chunks = splitter.splitWavFileIntoChunks(file);
            for (File chunk : chunks) {
                // Subsequent prompts are the previous transcriptions
                String transcription = transcribeChunk(prompt, chunk);
                // Assuming transcription is a String containing the JSON response

                transcriptions.add(transcription);
                prompt = transcription;

                // After transcribing, no longer need the chunk
                if (!chunk.delete()) {
                    logger.debug("Failed to delete " + chunk.getName());
                }
            }
        }

        // Join the individual transcripts and write to a file
        final String fileName = filePath.toString();
        final String transcription = String.join(" ", transcriptions);
    
        final String fileNameWithoutPath = fileName.substring(fileName.lastIndexOf("/") + 1);

        // Extract the file name without the extension
        String baseName = fileNameWithoutPath.contains(".") ? 
                          fileNameWithoutPath.substring(0, fileNameWithoutPath.lastIndexOf(".")) : 
                          fileNameWithoutPath;
        
        // Append .txt extension to the base name
        String newFileName = baseName + ".txt";
        
        fileUtils.writeTextToFile(transcription, newFileName);
        
        logger.debug(transcription);
        return transcription;
    }

    public void setTerminologyPromptFromContext(List<Context> contexts) throws IOException {
        final Path directory = Paths.get(TERMINOLOGY_GUIDE_FILES);

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        if( contexts == null || contexts.isEmpty() ) {
            setTerminologyPrompt(null);
            return;
        }

        // Get the history from the first available context
        Context latestContext = contexts.stream().max(Comparator.comparing(Context::getLastUsed))
                .orElse(null);
        final String assistantMessage = latestContext.getAssistantRoleMessage();

        final Path file =  Paths.get(TERMINOLOGY_GUIDE_FILES + removeSpacesAndCreateCamelString(assistantMessage) + ".txt");
        if (!Files.exists(file)) {
            setTerminologyPrompt(null);
            return;
        }
        if (assistantMessage != null) {
            // Read the text from a file and assign it to assistantMessage
            setTerminologyPrompt(readTextFromFile(file.toString()));
        }
    }

    private String readTextFromFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new APIError(HttpStatus.INTERNAL_SERVER_ERROR, filePath + " not found") ;
        }
        return content.toString();
    }

    private String removeSpacesAndCreateCamelString(String input) {
        String[] words = input.split(" ");
        StringBuilder camelCase = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim();
            if (!word.isEmpty()) {
                if (i == 0) {
                    camelCase.append(word.toLowerCase());
                } else {
                    camelCase.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
                }
            }
        }
        
        return camelCase.toString();    
}
}