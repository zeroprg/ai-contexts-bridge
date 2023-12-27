package com.bloberryconsulting.aicontextsbridge.security;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.User; // Assuming you have a User model
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository; // Your UserRepository

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String userId = oidcUser.getSubject(); // Unique identifier
        String userEmail = oidcUser.getEmail(); // User's email
        // Retrieve the user by ID or email
        User user = userRepository.findUserById(userId);
        if (user == null) {
            user = userRepository.findUserByEmail(userEmail);
        }

        // Common properties to update for both existing and new users
        Date currentLogin = new Date();

        if (user == null) {
            // Create a new user if not found
            user = new User();
            user.setEmail(oidcUser.getEmail());
            user.setId(userId);
            user.setName(oidcUser.getFullName());
            user.setRoles(new HashSet<>()); // Initialize with an empty set of roles
            if (oidcUser.getPicture() != null)
                user.setPictureLink(oidcUser.getPicture());
            userRepository.createUser(user);
            Collection<ApiKey> apiKeys = userRepository.findAllPublicApiKeys();
            if (apiKeys != null && !apiKeys.isEmpty()) {
                ApiKey defaultKey = apiKeys.stream().filter(ApiKey::isDefaultKey).findFirst().orElse(null);
                if (defaultKey != null) {
                    user.setRecentApiId(defaultKey.getKeyId());
                }
            }

        }
        // Update existing user details
        user.setId(userId); // Update the ID if necessary
        // all APIs will be updated with the new userId based on the email
        userRepository.findApiKeysByUserId(userEmail)
                .forEach(apiKey -> {
                    apiKey.setUserId(userId);
                    userRepository.updateApiKey(apiKey);
                });

        // Set the last login date for both new and existing users
        user.setLastLogin(currentLogin);
        userRepository.updateUser(user);

        // Map application-specific roles and authorities
        Set<GrantedAuthority> mappedAuthorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toSet());

        return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
