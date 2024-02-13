package com.bloberryconsulting.aicontextsbridge.apis.service.tools;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AudioProcessorConfig {

    // Inject all AudioProcessingService instances into this method
    @Bean
    public AudioProcessor audioProcessor(List<AudioProcessingService> audioProcessingServices) {
        return new AudioProcessor(audioProcessingServices);
    }
}