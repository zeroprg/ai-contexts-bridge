package com.bloberryconsulting.aicontextsbridge.controller;

import com.bloberryconsulting.aicontextsbridge.service.UserService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@RestController
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    private UserService userService;

    public StripeWebhookController(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        // Initialize Stripe API Key
        Stripe.apiKey = stripeSecretKey;
    }


    @PostMapping("/stripepayment/webhook")
        public synchronized ResponseEntity<String> processStripeEvent(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
            try {
                Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

                if ("checkout.session.completed".equals(event.getType())) {
                    // Assuming the event data is correctly mapped to a session object.
                    String sessionJsonString = event.getData().getObject().toJson();
                    log.debug("Session data: {}", sessionJsonString);
                    JSONObject sessionJson = new JSONObject(sessionJsonString);

                    if (sessionJson != null && sessionJson.has("id")) {
                        handleSession(sessionJson);
                    } else {
                        throw new IllegalStateException("Session data not found in the event.");
                    }
                }

                return ResponseEntity.ok("Event processed successfully.");
            } catch (StripeException | IllegalStateException e) {
                log.error("Error processing Stripe event.", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing Stripe event.");
            }
        }
    private void handleSession(JSONObject sessionJson) {
        try {
            // Extract customer email
            String customerEmail = sessionJson.has("customer_details") 
                                   && sessionJson.getJSONObject("customer_details").has("email")
                                   ? sessionJson.getJSONObject("customer_details").getString("email")
                                   : null;

            // Extract amount total and convert to dollars (assuming the amount is in cents)
            double amountTotal = sessionJson.has("amount_total")
                                 ? sessionJson.getLong("amount_total") / 100.0
                                 : 0.0;

            // Update user credit if email is present
            if (customerEmail != null) {
                userService.updateCredit(customerEmail, amountTotal);
            } else {
                log.warn("Customer email not found in session data.");
            }

        } catch (Exception e) {
            log.error("Error processing session object: {}", e.getMessage(), e);
        }
    }
} 