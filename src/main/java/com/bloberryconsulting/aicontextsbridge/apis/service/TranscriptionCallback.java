package com.bloberryconsulting.aicontextsbridge.apis.service;

import java.io.IOException;

import org.springframework.web.socket.WebSocketSession;

public interface TranscriptionCallback {
    void onTranscriptionResult( WebSocketSession session, String transcription) throws IOException;
    void onError(Exception e);
}
