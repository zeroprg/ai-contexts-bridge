package com.bloberryconsulting.aicontextsbridge.service;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class UserService {

    
    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, ResourceLoader resourceLoader, ObjectMapper objectMapper, CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    public void init() {
        initUsers();
        initApiKeys();
    }

    private  void initUsers() {
        try {
            Resource resource = resourceLoader.getResource("classpath:users.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("users.json file not found in classpath");
            }

            InputStream inputStream = resource.getInputStream();
            List<User> users = objectMapper.readValue(inputStream, new TypeReference<List<User>>() {});

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
            Resource resource = resourceLoader.getResource("classpath:apikeys.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("apikeys.json file not found in classpath");
            }

            InputStream inputStream = resource.getInputStream();
            List<ApiKey> apiKeys = objectMapper.readValue(inputStream, new TypeReference<List<ApiKey>>() {});

            apiKeys.forEach(apiKey -> {
                User user = userRepository.findUserByEmail(apiKey.getUserId());

                if ( user != null) {
                // Associate the API key with the users found by email     
                // You may want to set the API key's user ID or other relevant fields here
                    apiKey.setUserId(user.getId());
                    final String keyId =  UUID.randomUUID().toString();
                    apiKey.setKeyId(keyId);
                    try {
                        apiKey.setKeyValue(cryptoService.encrypt(apiKey.getKeyValue()));
                    } catch (Exception e) {
                                    // Log the exception details if necessary
                    e.printStackTrace(); // Consider replacing this with proper logging
                    // Throw an APIError instead of a generic RuntimeException
                    throw new APIError(HttpStatus.BAD_REQUEST, "Error encrypting api key. Please check the encryption key.");
                    }
                    
                    userRepository.saveApiKey(apiKey); 
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            // Handle the specific case when apikeys.json is not found
        } catch (IOException e) {
            e.printStackTrace();
            // Handle other exceptions related to reading or processing the JSON file
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

}

