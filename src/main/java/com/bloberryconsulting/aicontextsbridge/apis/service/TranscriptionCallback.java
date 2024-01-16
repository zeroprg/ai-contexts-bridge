package com.bloberryconsulting.aicontextsbridge.apis.service;

import java.io.IOException;

public interface TranscriptionCallback {
    void onTranscriptionResult(String transcription) throws IOException;
    void onError(Exception e);
}
