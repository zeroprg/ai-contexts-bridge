package com.bloberryconsulting.aicontextsbridge.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.servlet.ServletException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // Corrected import
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import javax.servlet.http.Cookie;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
 
    @Value("${ui.uri}")
    private String uiUri;
    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // Removed the call to super.onAuthenticationSuccess() as it's unnecessary here
        String targetUrl = determineTargetUrl(request, response, authentication);
        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Set the token in an HTTP-only cookie
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            String token = getAccessToken(oauthToken);
            String sessionID = request.getSession(false) != null ? request.getSession().getId() : "No session";
            Cookie cookieToken = new Cookie("token", token);
            cookieToken.setHttpOnly(false);
            cookieToken.setPath("/");
            // Set other cookie properties as needed (e.g., secure, maxAge, domain)
            response.addCookie(cookieToken);
            Cookie sessionIDCookie = new Cookie("sessionId", sessionID);
            sessionIDCookie.setHttpOnly(false);
            sessionIDCookie.setPath("/");
            response.addCookie(sessionIDCookie);
        }
    
        // Determine redirect URL based on the referer or other logic
        String refererUrl = request.getHeader("Referer");
        if (refererUrl != null && refererUrl.contains("/swagger-ui")) {
            return refererUrl; // Redirect back to Swagger UI
        }
        return uiUri; // Redirect to frontend UI
    }
    

// Method to extract token from the Authentication object
// This method needs to be implemented based on how you're storing the token
private String getAccessToken(OAuth2AuthenticationToken oauthToken) {
    OAuth2AuthorizedClient client = authorizedClientService
            .loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName());

    return client == null ? null : client.getAccessToken().getTokenValue();
}

}

