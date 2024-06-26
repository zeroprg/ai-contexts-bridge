package com.bloberryconsulting.aicontextsbridge.service;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final String opeAPIkey;

    public UserService(UserRepository userRepository, ResourceLoader resourceLoader, ObjectMapper objectMapper,
            CryptoService cryptoService, @Value("${user-service.OPENAI_API_KEY}") String opeAPIkey) {
        this.userRepository = userRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
        this.opeAPIkey = opeAPIkey;
    }

    @PostConstruct
    public void init() {
        initUsers();
        initApiKeys();
    }

    private void initUsers() {
        try {
            Resource resource = resourceLoader.getResource("classpath:users.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("users.json file not found in classpath");
            }

            InputStream inputStream = resource.getInputStream();
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {
            });

            users.forEach(user -> {
                if (userRepository.findUserByEmail(user.getEmail()) == null) {
                    userRepository.createUser(user);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Handle the specific case when users.json is not found
        } catch (Exception e) {
            e.printStackTrace();
            // Handle other exceptions
        }
    }

    private void initApiKeys() {

        try {

            final String encoded = cryptoService.encrypt(opeAPIkey);

            Resource resource = resourceLoader.getResource("classpath:apikeys.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("apikeys.json file not found in classpath");
            }

            InputStream inputStream = resource.getInputStream();
            List<ApiKey> apiKeys = objectMapper.readValue(inputStream, new TypeReference<List<ApiKey>>() {
            });

            apiKeys.forEach(apiKey -> {
                User user = userRepository.findUserByEmail(apiKey.getUserId());

                if (user != null) {
                    // Associate the API key with the users found by email
                    // You may want to set the API key's user ID or other relevant fields here
                    apiKey.setUserId(user.getId());

                    final String keyId = UUID.randomUUID().toString();
                    apiKey.setKeyId(keyId);
                    if (apiKey.isDefaultKey())
                        user.setRecentApiId(apiKey.getKeyId());// always set the most recent API key as the default
                    if (apiKey.getName() == null || "OpenAI".equals(apiKey.getName())) {
                        apiKey.setKeyValue(encoded);
                    } else
                        try {
                            apiKey.setKeyValue(cryptoService.encrypt(apiKey.getKeyValue()));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                    userRepository.saveApiKey(apiKey);
                    userRepository.updateUser(user);
                }
            });
        } catch (Exception e) {
            // Log the exception details if necessary
            e.printStackTrace(); // Consider replacing this with proper logging
            // Throw an APIError instead of a generic RuntimeException
            throw new APIError(HttpStatus.BAD_REQUEST,
                    "Error encrypting api key. Please check the encryption key or file not found exception");
        }

    }

    public User findUserByEmail(String email) {
        // Implement logic to find a user by email
        return userRepository.findUserByEmail(email);
    }

    public User findUserByName(String name) {
        // Implement logic to find a user by name
        return userRepository.findUserByName(name);
    }

    public User getUserById(String id) {
        // Implement logic to find a user by id
        return userRepository.findUserById(id);
    }

    /**
     * Updates the user's last login date.
     * 
     * @param recentAPI
     * @param userId
     */
    public void updateUserAPI(String userId, String recentAPI) {
        User user = userRepository.findUserById(userId);
        user.setRecentApiId(recentAPI);
        userRepository.updateUser(user);
    }

    public void assignRoleToUser(String email, String role) {
        // Implement logic to assign a role to a user
        User user = userRepository.findUserByEmail(email);
        if (user != null) {
            user.getRoles().add(role);
            SecurityUtils.addRoleToCurrentUserIfNotExists(role);
            userRepository.updateUser(user);
        }
    }

    public void updateUser(User user) {
        // Implement logic to update a user
        userRepository.updateUser(user);
    }

    public void updateUsersContexts(User user, Context context) {
        // Implement logic to update a user

        Map<String, Context> contexts = user.getContexts();

        if (contexts == null) {
            contexts = new HashMap<String, Context>();
        }
        contexts.put(context.getName(), context);
        user.setContexts(contexts);
        this.updateUser(user);

    }

    public void updateUsersContexts(User user, List<Context> contexts) {
        // Implement logic to update a user

        Map<String, Context> userContexts = user.getContexts();

        if (userContexts == null) {
            userContexts = new HashMap<String, Context>();
        }
        for (Context context : contexts) {
            userContexts.put(context.getName(), context);
            user.setContexts(userContexts);
        }
        this.updateUser(user);

    }

    public List<Context> getUserContextById(User user, String sessionId) {
        // Implement logic to update a user

        Map<String, Context> contexts = user.getContexts();

        if (contexts != null) {
            return contexts.entrySet().stream()
                    .filter(entry -> entry.getValue().getSessionId().equals(sessionId))
                    .map(Map.Entry::getValue).collect(Collectors.toList());
        }
        return null;
    }


    /**
     * Update the user's credit based on their email.
     * @param userEmail The email of the user.
     * @param amountToAdd The amount to add to the user's credit.
     */
    public void updateCredit(String userEmail, double amountToAdd) {
        try {
            User user = userRepository.findUserByEmail(userEmail); // Assuming findByEmail is a method in your repository

            if (user != null) {
                double currentCredit = user.getCredit();
                double updatedCredit = currentCredit - amountToAdd;
                user.setCredit(updatedCredit);

                userRepository.saveUser(user); // Persist the updated user entity
                log.info("Updated credit for user with email {}: new credit is {}", userEmail, updatedCredit);
            } else {
                log.warn("User with email {} not found.", userEmail);
                // Handle the case where the user is not found
            }
        } catch (Exception e) {
            log.error("Error updating credit for user with email {}: {}", userEmail, e.getMessage(), e);
            // Handle any exceptions
        }
    }

    public boolean deleteFile(User user, String sessionId, String fileId) {
        Map<String, Context> contexts = user.getContexts();

        if (contexts != null) {
            Optional<String> contextKey = contexts.entrySet().stream()
                    .filter(entry -> fileId.equals(entry.getKey()) && entry.getValue().getSessionId().equals(sessionId))
                    .map(Map.Entry::getKey)
                    .findFirst();

            if (contextKey.isPresent()) {
                contexts.remove(contextKey.get());
                return true; // Return true to indicate successful deletion
            }
        }
        return false; // Return false if the context was not found or if the contexts map is null
    }

}
