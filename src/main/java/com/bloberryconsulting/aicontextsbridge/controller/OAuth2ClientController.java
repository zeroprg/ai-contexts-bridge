package com.bloberryconsulting.aicontextsbridge.controller;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuth2ClientController {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2ClientController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/oauth2tokens")
    public String getOAuth2Tokens(HttpServletRequest request, OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        if (client == null) {
            return "No OAuth2 client found for the current user";
        }

        String accessToken = client.getAccessToken().getTokenValue();

        // Getting the session ID from the HttpServletRequest
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";

        return "Access Token: " + accessToken + ", Session ID: " + sessionId;
    }
}
