package com.bloberryconsulting.aicontextsbridge.apis.service.billing;

import java.util.HashMap;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;

import org.aspectj.lang.annotation.Around;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.service.CryptoService;

import org.springframework.stereotype.Component;

@Aspect
@Component
public class BillingAspect {
    private static final double TAX = 0.05;
    private final CryptoService cryptoService;
    private final UserRepository userRepository;

    public BillingAspect(CryptoService cryptoService, UserRepository userRepository) {
        this.cryptoService = cryptoService;
        this.userRepository = userRepository;
    }

    @Around("execution(* com.bloberryconsulting.aicontextsbridge.apis.service.ApiService.getResponse(..))")
    public String billAfterServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract necessary information for billing
        // This can be the API key, token count, etc.
        final ApiKey apiKeyObject = (ApiKey) joinPoint.getArgs()[0];
        final String message = joinPoint.getArgs()[1].toString();
        final List<Context> contexts = (List<Context>) joinPoint.getArgs()[2];

        String apiURI = apiKeyObject.getUri();
        final String encodedApiKey = apiKeyObject.getKeyValue();
        final String decodedApiKey = decodeApiKey(apiKeyObject);
        apiKeyObject.setKeyValue(decodedApiKey);
        if (apiURI == null) {
            apiURI = apiKeyObject.getUri();
        }

        String response = (String) joinPoint.proceed(new Object[] { apiKeyObject, message, contexts }); // Call the API
                                                                                                        // service

        int tokenCount = calculateTokenCount(response) + calculateTokenCount(message);

        // Perform billing calculation
        double totalCost = calculateTotalCost(tokenCount);

        String userId = contexts.get(0).getUserId();
        // You can store the bill information or send it to another service for handling
        // find user and update it credit
        User user = userRepository.findUserById(userId);
        user.setCredit(totalCost + user.getCredit());
        // Ensure that the user's contexts map is initialized
        if (user.getContexts() == null) {
            user.setContexts(new HashMap<>());
        }
        // Use Java 8 forEach to populate the map
        contexts.forEach(context -> user.getContexts().put(context.getName(), context));
        userRepository.saveUser(user);
        apiKeyObject.setKeyValue(encodedApiKey);
        apiKeyObject.setTotalCost(totalCost + apiKeyObject.getTotalCost());
        userRepository.updateApiKey(apiKeyObject);

        return response;

    }

    // Logic to decode API by secret and strong decode algorithm
    private String decodeApiKey(ApiKey apiKey) throws Exception {
        String decrypted = cryptoService.decrypt(apiKey.getKeyValue());
        return decrypted;
    }

    private int calculateTokenCount(Object response) {
        if (response instanceof String) {
            String responseString = (String) response;
            // Simple approximation: split by spaces (not accurate for all languages and
            // cases)
            return responseString.split("\\s+").length;
        }
        return 0; // If the response is not a string or cannot be tokenized
    }

    private double calculateTotalCost(int tokenCount) {
        // Simple approximation: 1 token = $0.001
        return tokenCount * 0.001;
    }
}
