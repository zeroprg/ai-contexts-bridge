package com.bloberryconsulting.aicontextsbridge.apis.service.billing;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Bill;
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
    public void billAfterServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract necessary information for billing
        // This can be the API key, token count, etc.
        final String userId = (String)joinPoint.getArgs()[0];
        final String apiKey = (String)joinPoint.getArgs()[1];
        String apiURI = (String)joinPoint.getArgs()[2];
        final String message = joinPoint.getArgs()[3].toString();
        
        final ApiKey apiKeyObject = userRepository.findApiKeyByApiKeyId(apiKey);
        final String decodedApiKey = decodeApiKey(apiKeyObject);
        if( apiURI == null ){
            apiURI = apiKeyObject.getUri();
        }
        Object response = joinPoint.proceed(new Object[]{userId, decodedApiKey, apiURI, message}); // Call the API service 

        int tokenCount = calculateTokenCount(response);

        // Perform billing calculation
        double totalCost = calculateTotalCost(tokenCount);
        // You can store the bill information or send it to another service for handling
        // find bill if it was not found create new one
        Bill bill = userRepository.findBillByUserId(userId);
        if(bill == null){
            String billId = "bill-" + userId;
            bill = new Bill(billId, totalCost, TAX);
            userRepository.createBill(bill);
        } else {         
            bill.setTotalCost(bill.getTotalCost() + totalCost);         
        }
       
        apiKeyObject.setTotalCost(apiKeyObject.getTotalCost() + totalCost);
        userRepository.updateApiKey(apiKeyObject);
        
    }

    // Logic to decode API by secret and strong decode algorithm
    private String decodeApiKey(ApiKey apiKey) throws Exception {    
        String decrypted = cryptoService.decrypt(apiKey.getKeyValue()); 
        return decrypted;
    }  

    private int calculateTokenCount(Object response) {
        if (response instanceof String) {
            String responseString = (String) response;
            // Simple approximation: split by spaces (not accurate for all languages and cases)
            return responseString.split("\\s+").length;
        }
        return 0; // If the response is not a string or cannot be tokenized
    }

    private double calculateTotalCost(int tokenCount) {
        // Simple approximation: 1 token = $0.001
        return tokenCount * 0.001;
    }
}