package com.bloberryconsulting.aicontextsbridge.apis.service.utilities;

import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    public static final String TEXT_RESOURCES_PATH = "./text";
    public static final String IMAGE_RESOURCES_PATH = "./images";
    public static final String AUDIO_RESOURCES_PATH = "./audio_files";
    private static int counter;

    // Initialize directories
    static {
        createDirectories(TEXT_RESOURCES_PATH, IMAGE_RESOURCES_PATH);
    }

    // Simplifies reading files to a single line
    public static String readFile(String fileName) throws IOException {
        return Files.readString(Path.of(fileName));
    }

    // Consolidates image writing methods
    public Path writeImageToFile(String imageData, boolean isBase64Encoded) throws IOException {
        byte[] bytes = isBase64Encoded ? Base64.getDecoder().decode(imageData) : imageData.getBytes();
        return writeBytesToFile(bytes, IMAGE_RESOURCES_PATH, "image", "png");
    }

    // Generalizes writing bytes to files
    public Path writeBytesToFile(byte[] bytes, String resourcePath, String filePrefix, String extension) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileName = String.format("%s_%s_%d.%s", filePrefix, timestamp, counter++, extension);
        Path filePath = ensureDirectoryExists(resourcePath).resolve(fileName);
        Files.write(filePath, bytes, StandardOpenOption.CREATE_NEW);
        logger.debug("Saved {} to {}", fileName, resourcePath);
        return filePath;
    }
    
    // Handles writing sound bytes to a specific file
    public Path writeSoundBytesToGivenFile(byte[] bytes) throws IOException {
        return writeBytesToFile(bytes, AUDIO_RESOURCES_PATH, "audio","mp3");

    }

    // Writes text data to a file, creating or overwriting as necessary
    public void writeTextToFile(String textData, String fileName) throws IOException {
        Path filePath = ensureDirectoryExists(TEXT_RESOURCES_PATH).resolve(fileName);
        Files.writeString(filePath, textData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        logger.debug("Saved {} to {}", fileName, TEXT_RESOURCES_PATH);
    }

    // Refactored to streamline document creation and writing
    public void writeWordDocument(Map<String, String> data) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            data.forEach((key, value) -> processDocumentEntries(document, key, value));
            writeDocumentToFile(document, TEXT_RESOURCES_PATH + "/document.docx");
        }
    }

    // Helper methods
    private static Path ensureDirectoryExists(String path) throws IOException {
        Path directory = Paths.get(path);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        return directory;
    }

    private static void createDirectories(String... paths) {
        Arrays.stream(paths).forEach(path -> {
            try {
                Files.createDirectories(Paths.get(path));
            } catch (IOException e) {
                throw new UncheckedIOException("Error creating directory", e);
            }
        });
    }

    private void processDocumentEntries(XWPFDocument document, String key, String value) {
        String title = transformKeyToTitle(key);
        addTitleToWordDocument(document, title);
        addTextToWordDocument(document, value);
    }

    private static String transformKeyToTitle(String key) {
        return Arrays.stream(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    private static void addTitleToWordDocument(XWPFDocument document, String title) {
        XWPFRun titleRun = document.createParagraph().createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(16);
        titleRun.setText(title);
    }

    private static void addTextToWordDocument(XWPFDocument document, String text) {
        Pattern pattern = Pattern.compile("^\\d+\\. +.*");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            XWPFNumbering numbering = document.createNumbering();
            BigInteger numId = numbering.addNum(numbering.getAbstractNumID(BigInteger.ONE));

            text.lines().forEach(line -> {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setNumID(numId);
                paragraph.createRun().setText(line.substring(line.indexOf('.') + 1).trim());
            });
        } else {
            document.createParagraph().createRun().setText(text);
        }
    }

    private static void writeDocumentToFile(XWPFDocument document, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            document.write(fos);
        }
    }
}