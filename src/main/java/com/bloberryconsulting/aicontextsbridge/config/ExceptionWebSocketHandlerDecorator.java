package com.bloberryconsulting.aicontextsbridge.config;

import java.io.IOException;


import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

import com.bloberryconsulting.aicontextsbridge.exceptions.GlobalExceptionHandler;

public class ExceptionWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

    private final GlobalExceptionHandler globalExceptionHandler;
    //private static final Logger logger = org.slf4j.LoggerFactory.getLogger(ExceptionWebSocketHandlerDecorator.class);

    // Constructor to inject the global exception handler
    public ExceptionWebSocketHandlerDecorator(WebSocketHandler delegate, GlobalExceptionHandler globalExceptionHandler) {
        super(delegate);
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        try {
            getDelegate().handleMessage(session, message);
        }
        catch (Exception ex) {
            // Redirect the exception to the global exception handler
            globalExceptionHandler.handleWebSocketException(session, ex);
           
        }
    }

}
