package com.bloberryconsulting.aicontextsbridge.controller;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StripeController {
    @Value("${stripe.secret.key}")
    private String stripeSecretKey; 
    @Value("${ui.uri}")
    private String ui_domain;

    @PostConstruct
    public void setup() {
        Stripe.apiKey = stripeSecretKey; // Set your Stripe secret key
    }

    @PostMapping("/stripepayment/create-checkout-session")
    public Map<String, Object> createSession(@RequestBody Map<String, Object> data) {
        try {
            // Retrieve the current user's ID
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserId = authentication.getName(); // Assuming this retrieves the user ID

            int amount = (int) data.get("amount");
            // Replace with actual price IDs from Stripe Dashboard
            String priceId = getPriceIdForAmount(amount);

            SessionCreateParams params = SessionCreateParams.builder()
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(ui_domain +"/payment-success")
                    //.setCancelUrl(ui_domain + "/payment-cancel")
                    .putMetadata("userId", currentUserId) // Include user ID in metadata
                    .build();

            Session session = Session.create(params);
            Map<String, Object> response = new HashMap<>();
            response.put("id", session.getId());
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Stripe Checkout Session creation failed.");
        }
    }


    private String getPriceIdForAmount(int amount) {
        Map<Integer, String> priceMap = new HashMap<>();
        priceMap.put(1, "price_1OReWjFJuJWNHOtRF0QT4RZW");
        priceMap.put(3, "price_1OReY9FJuJWNHOtRUPgSubGJ");
        priceMap.put(10, "price_1ORedVFJuJWNHOtRtqnauueP");
        // Add more mappings as needed
        return priceMap.get(amount);
    }
}
