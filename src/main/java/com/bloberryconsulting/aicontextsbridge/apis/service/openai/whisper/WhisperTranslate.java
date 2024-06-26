package com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper;

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
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

// See docs at https://platform.openai.com/docs/api-reference/audio/createTranslation
@Service
public class WhisperTranslate {
    private final static String URL = "https://api.openai.com/v1/audio/translations";
    public final static int MAX_ALLOWED_SIZE = 25 * 1024 * 1024;

    private final FileUtils fileUtils;

    private final static String KEY = System.getenv("OPENAI_API_KEY");

    // Only model available as of Fall 2023 is whisper-1
    private final static String MODEL = "whisper-1";

    public WhisperTranslate(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }   

    public static final String WORD_LIST = String.join(", ",
            List.of("Kousen", "GPT-3", "GPT-4", "DALL-E",
                    "Midjourney", "AssertJ", "Mockito", "JUnit", "Java", "Kotlin", "Groovy", "Scala",
                    "IOException", "RuntimeException", "UncheckedIOException", "UnsupportedAudioFileException",
                    "assertThrows", "assertTrue", "assertEquals", "assertNull", "assertNotNull", "assertThat",
                    "Tales from the jar side", "Spring Boot", "Spring Framework", "Spring Data", "Spring Security"));

    private String translateChunk(String prompt, File chunkFile) {
        System.out.printf("Translating %s%n", chunkFile.getName());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Authorization", "Bearer %s".formatted(KEY));

            HttpEntity entity = MultipartEntityBuilder.create()
                    .setContentType(ContentType.MULTIPART_FORM_DATA)
                    .addPart("file", new FileBody(chunkFile, ContentType.DEFAULT_BINARY))
                    .addPart("model", new StringBody(MODEL, ContentType.DEFAULT_TEXT))
                    .addPart("response_format", new StringBody("text", ContentType.DEFAULT_TEXT))
                    .addPart("language", new StringBody("en", ContentType.DEFAULT_TEXT))
                    .addPart("prompt", new StringBody(prompt, ContentType.DEFAULT_TEXT))
                    .build();
            httpPost.setEntity(entity);

            return client.execute(httpPost, response -> {
                System.out.println("Status: " + new StatusLine(response));
                return EntityUtils.toString(response.getEntity());
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String translate(String fileName) throws IOException {
        File file = new File(fileName);

        // Collect the translations of each chunk
        List<String> translations = new ArrayList<>();

        // First prompt is the word list
        String prompt = WORD_LIST;

        if (file.length() <= MAX_ALLOWED_SIZE) {
            String translation = translateChunk(prompt, file);
            translations = List.of(translation);
        } else {
            var splitter = new WavFileSplitter();
            List<File> chunks = splitter.splitWavFileIntoChunks(file);
            for (File chunk : chunks) {
                // Subsequent prompts are the previous translations
                String translation = translateChunk(prompt, chunk);
                translations.add(translation);
                prompt = translation;

                // After transcribing, no longer need the chunk
                if (!chunk.delete()) {
                    System.out.println("Failed to delete " + chunk.getName());
                }
            }
        }

        // Join the individual transcripts and write to a file
        String transcription = String.join(" ", translations);
        String fileNameWithoutPath = fileName.substring(
                fileName.lastIndexOf("/") + 1);
        fileUtils.writeTextToFile(transcription,
                fileNameWithoutPath.replace(".wav", ".translation.txt"));
        return transcription;
    }
}