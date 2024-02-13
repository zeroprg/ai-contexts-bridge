package com.bloberryconsulting.aicontextsbridge.apis.service;

import java.io.FileInputStream;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.threeten.bp.Duration;

import com.google.api.core.ApiFunction;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechSettings;

/**
 * Abstract class to be extended by all Google API services
 */
public abstract class AbstractGoogleAPIs {

    protected SpeechSettings speechSettings;

   
    @Value("${google.credentials.file.path}")
    private String apiKeyFilePath;

    @PostConstruct
    public void init() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(apiKeyFilePath))
            .createScoped(SpeechSettings.getDefaultServiceScopes());

        RetrySettings retrySettings = RetrySettings.newBuilder()
            .setInitialRetryDelay(Duration.ofMillis(100))
            .setMaxRetryDelay(Duration.ofSeconds(10))
            .setRetryDelayMultiplier(2.0)
            .setTotalTimeout(Duration.ofMinutes(1))
            .setMaxAttempts(10)
            .build();

        ApiFunction<UnaryCallSettings.Builder<?, ?>, Void> retrySettingsApplier = 
            builder -> {
                builder.setRetrySettings(retrySettings);
                return null;
            };


        this.speechSettings = SpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            // Apply the retry settings to all RPCs (or specific RPCs as needed)
            .applyToAllUnaryMethods(retrySettingsApplier)
            .build();
    }

    // Other abstract methods or concrete methods can be added here
}
