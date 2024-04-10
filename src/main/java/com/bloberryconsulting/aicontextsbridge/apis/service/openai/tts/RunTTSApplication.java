package com.bloberryconsulting.aicontextsbridge.apis.service.openai.tts;

import com.bloberryconsulting.aicontextsbridge.apis.service.openai.json.Voice;

public class RunTTSApplication {
    public static void main(String[] args) {
        TextToSpeech tts =  null;// new TextToSpeech();
        try (var scanner = new java.util.Scanner(System.in)) {
            System.out.println("Enter text to convert to speech: ");
            String text = scanner.nextLine();
            tts.createAndPlay(text, Voice.getRandomVoice());
        }
    }
}
