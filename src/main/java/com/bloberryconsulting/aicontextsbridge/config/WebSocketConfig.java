package com.bloberryconsulting.aicontextsbridge.config;


import org.springframework.context.annotation.Configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import com.bloberryconsulting.aicontextsbridge.exceptions.GlobalExceptionHandler;

@Deprecated
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Value("${ui.uri}")
    private String uiUri;
   
    private final WebSocketHandler webSocketHandler;    
    private final GlobalExceptionHandler globalExceptionHandler;


    public WebSocketConfig(WebSocketHandler webSocketHandler, GlobalExceptionHandler globalExceptionHandler) {
        this.webSocketHandler = webSocketHandler;
        this.globalExceptionHandler = globalExceptionHandler;
    }   

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(decorateWebSocketHandler(webSocketHandler), "/ws/audio")
        .setAllowedOrigins(uiUri); // Your client's URL

    }



    private WebSocketHandlerDecorator decorateWebSocketHandler(WebSocketHandler handler) {
        // Wrap your handler with the custom decorator
        return new ExceptionWebSocketHandlerDecorator(handler, globalExceptionHandler);
    }


}