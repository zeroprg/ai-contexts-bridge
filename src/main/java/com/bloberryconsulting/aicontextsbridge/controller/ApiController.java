package com.bloberryconsulting.aicontextsbridge.controller;

import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_CLIENT_ADMINISTRATOR_DESC;
import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_CUSTOMER_DESCR;
import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_SITE_ADMINISTRATOR_DESCR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bloberryconsulting.aicontextsbridge.apis.service.ApiService;
import com.bloberryconsulting.aicontextsbridge.apis.service.ApiServiceRegistry;
import com.bloberryconsulting.aicontextsbridge.apis.service.openai.whisper.WhisperTranscribe;
import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Client;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.repository.UserRepository;
import com.bloberryconsulting.aicontextsbridge.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "${ui.uri}", allowCredentials = "true")
public class ApiController {

    private static final String DEFAULT_ASSISTANCE_ROLE_MESSAGE = "Technical assistant";
    static final String DIRECTORY_TO_AUDIO = "audio_files";
    static final String DEFAULT_AUDIO_FILE = "audio";
    private static final double AUDIO_TARIF = 0.006; // Cost per minute

    private final UserRepository userRepository;
    private final ApiServiceRegistry apiServiceRegistry;
    private final UserService userService;
    // Assuming whisperTranscribe is a service for transcription
    private final WhisperTranscribe whisperTranscribe;

    public ApiController(UserRepository userRepository, ApiServiceRegistry apiServiceRegistry, UserService userService,
            WhisperTranscribe whisperTranscribe) {
        this.userRepository = userRepository;
        this.apiServiceRegistry = apiServiceRegistry;
        this.userService = userService;
        this.whisperTranscribe = whisperTranscribe;
    }

    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @PostMapping("/transcription")
    @Operation(summary = "Upload audio for transcription", description = "Receives an audio file and returns its transcription."
            +
            ROLE_CUSTOMER_DESCR, tags = { "Audio Transcription" }, security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully transcribed the audio", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid audio file", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> uploadAudio(HttpServletRequest request,
            @RequestParam("audioFile") MultipartFile audioFile) {
        Path savePath = null;
        final String transcription;
        try {
            // Define the directory path where files will be saved
            final Path directory = Paths.get(DIRECTORY_TO_AUDIO);
            // String mimeType = audioFile.getContentType();
            String originalName = audioFile.getOriginalFilename();
            final String fileName = originalName.equals(DEFAULT_AUDIO_FILE) ? System.currentTimeMillis() + "_"
                    : originalName;
            // Ensure directory exists
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // Calculate the duration of the audio file in minutes
            double price = getAudioDurationInMinutes(audioFile) * AUDIO_TARIF;

            // Save the file
            savePath = Paths.get(DIRECTORY_TO_AUDIO, fileName);
            Files.copy(audioFile.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            // Retrieve context documents associated with the user and session
            List<Context> contexts = getContexts(request, price); // Get the contexts from the request
            whisperTranscribe.setTerminologyPromptFromContext(contexts); // Set the terminology prompt if necessary
            // Process the file for transcription asynchronously or as required
            transcription = whisperTranscribe.transcribe(savePath);

        } catch (Exception e) {
            throw new APIError(HttpStatus.EXPECTATION_FAILED, "Error saving or processing audio file.");
        } finally {
            // Clean up resources if necessary
            deleteAudioFile(savePath);
        }
        return ResponseEntity.ok(transcription);
    }

    private void deleteAudioFile(Path savePath) {
        if (savePath == null || !Files.exists(savePath)) {
            return;
        }
        try {
            Files.deleteIfExists(savePath);
        } catch (IOException e) {
            throw new APIError(HttpStatus.EXPECTATION_FAILED, "Error deleting or temporary audio file.");
        }
    }

    private double getAudioDurationInMinutes(MultipartFile audioFile) {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile.getInputStream())) {
            // Assuming the audio format and frame rate are supported, calculate the
            // duration
            long frames = audioInputStream.getFrameLength();
            float frameRate = audioInputStream.getFormat().getFrameRate();
            return (frames / frameRate) / 60.0;
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Operation(summary = "View only my API keys (owned by the current user) or his company (client) or public APIs ", description = "Retrieves a list of API keys that are owned by the currently authenticated user or his company (client). No special role requiered.",

            security = @SecurityRequirement(name = "oauth2scheme"), tags = { "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/keys/manage")
    public ResponseEntity<?> viewApiKeys() {
        // Retrieve the current user's ID
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserId = authentication.getName(); // Assuming this retrieves the user ID

        // Filter and retrieve API keys where userId matches the current user's ID
        Collection<ApiKey> userApiKeys = userRepository.findApiKeysByUserId(currentUserId);

        return ResponseEntity.ok(userApiKeys);
    }

    // Example for another method
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Create a new API key", description = "Creates a new API key with the specified details and assigns it to the current user."
            +
            ROLE_CUSTOMER_DESCR, security = @SecurityRequirement(name = "oauth2scheme"), tags = {
                    "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/keys/manage")
    public ResponseEntity<?> createApiKey(@RequestBody ApiKey apiKey) {
        // Logic to create a new API key and assign roles
        final String keyId = UUID.randomUUID().toString();
        apiKey.setKeyId(keyId);
        userRepository.saveApiKey(apiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKey);
    }

    @PreAuthorize("hasAuthority('ROLE_SITE_ADMINISTRATOR')")
    @Operation(summary = "View all client profiles", description = "Retrieves a list of all client profiles in the system, accessible only by site administrators."
            +
            ROLE_SITE_ADMINISTRATOR_DESCR, security = @SecurityRequirement(name = "oauth2scheme"), tags = {
                    "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client/profiles")
    public ResponseEntity<?> viewAllProfiles() {
        // Logic to retrieve all client profiles
        return ResponseEntity.ok(userRepository.findAllClientProfiles());
    }

    @PreAuthorize("hasAuthority('ROLE_SITE_ADMINISTRATOR')")
    @Operation(summary = "View a specific client profile", description = "Retrieves the profile details for a specific client, identified by the client ID ."
            +
            ROLE_SITE_ADMINISTRATOR_DESCR, security = @SecurityRequirement(name = "oauth2scheme"), tags = {
                    "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/client/{clientId}/profile")
    public ResponseEntity<?> viewClientProfile(@PathVariable String clientId) {
        // Logic to retrieve a specific client profile
        return ResponseEntity.ok(userRepository.findProfileByClientId(clientId));
    }

    @PreAuthorize("hasAuthority('ROLE_CLIENT_ADMINISTRATOR')")
    @Operation(summary = "Create a new client profile", description = "Creates a new client profile or updates an existing one, based on the provided client ID and profile details."
            +
            ROLE_CLIENT_ADMINISTRATOR_DESC, security = @SecurityRequirement(name = "oauth2scheme"), tags = {
                    "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/client/profile")
    public ResponseEntity<?> manageClientProfile(@RequestBody Client client) {
        // Logic to create a client profile
        // Logic to retrieve personalized responses for a customer
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String currentUserId = authentication.getName(); // Assuming this retrieves the user ID
        User user = userRepository.findUserById(currentUserId);
        final String clientId = UUID.randomUUID().toString();
        user.setClientId(clientId);
        client.setOwnerId(user.getId());
        userRepository.saveOrUpdateClient(clientId, client);
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    @PreAuthorize("hasAuthority('ROLE_CLIENT_ADMINISTRATOR')")
    @Operation(summary = "Update a specific client profile", description = "Updates the profile details for a specific client, identified by the client ID."
            +
            "This operation is only available to users with the 'ROLE_CLIENT_ADMINISTRATOR' role, which typically includes view/creation/update/query of customers/users of current client ", security = @SecurityRequirement(name = "oauth2scheme"), tags = {
                    "API's CRUD operations" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/client/profile")
    public ResponseEntity<?> updateClient(@RequestBody Client client) {
        userRepository.saveOrUpdateClient(client.getId(), client);
        return ResponseEntity.ok(client);
    }

    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')")
    @Operation(summary = "Retrieve personalized API response for a customer", description = "Fetches a personalized response for a customer based on their unique customer ID and query message."
            +
            ROLE_CUSTOMER_DESCR,

            security = @SecurityRequirement(name = "oauth2scheme"), tags = { "Customer API Query" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved personalized responses", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "429", description = "Too Many Requests - Rate limit exceeded", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "User is not authenticated", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/customer/query")
    public ResponseEntity<?> queryCustomer(
            HttpServletRequest request,
            @Parameter(description = "The message to be processed by the API service") @RequestBody PayloadDTO payload) {

        // Retrieve user and session information
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        User user = userRepository.findUserById(currentUserId);
        if (user == null) {
            throw new APIError(HttpStatus.BAD_REQUEST, "No user found for the current user ID: " + currentUserId);
        } else if (user.getCredit() > 5.0 && !user.getRoles().contains("ROLE_APIKEY_MANAGER")) {
            throw new APIError(HttpStatus.BAD_REQUEST,
                    "Donate a few dollars. Credit is overlimit for the current user ID: " + user.getName() + " credit: "
                            + user.getCredit());
        }
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        if (payload.getSessionId() != null && !payload.getSessionId().isEmpty()) {
            sessionId = payload.getSessionId();
        }

        // Retrieve context documents associated with the user and session
        List<Context> contexts = userService.getUserContextById(user, sessionId);

        if (contexts == null || contexts.isEmpty()) {
            Context context = new Context();
            context.setSessionId(sessionId);
            context.setName("Default");
            context.setUserId(user.getId());
            context.setAssistantRoleMessage(DEFAULT_ASSISTANCE_ROLE_MESSAGE);
            contexts = new ArrayList<Context>();
            contexts.add(context);
        }

        // Continue with API key validation and service invocation
        String apiKey = user.getRecentApiId();
        if (apiKey == null) {
            throw new APIError(HttpStatus.BAD_REQUEST, "No API key selected");
        }

        ApiKey apiKeyObject = userRepository.findApiKeyByApiKeyId(apiKey);
        if (apiKeyObject == null) {
            throw new APIError(HttpStatus.BAD_REQUEST, "No API key found for the selected API apiKeyId: " + apiKey);
        }

        String result = null;
        ApiService apiService = apiServiceRegistry.getService(apiKeyObject.getName());

        result = apiService.getResponse(apiKeyObject, payload.getData(), contexts);

        return ResponseEntity.ok(result);
    }

    private List<Context> getContexts(HttpServletRequest request, Double credit) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        User user = userRepository.findUserById(currentUserId);
        final Double totalCredit = credit + user.getCredit();

        if (totalCredit > 1.0 && !user.getRoles().contains("ROLE_APIKEY_MANAGER")) {
            throw new APIError(HttpStatus.BAD_REQUEST,
                    "Donate a few dollars. Credit is overlimit for the current user ID: " + user.getName() + " credit: "
                            + user.getCredit());
        }
        if (credit > 0) {
            user.setCredit(totalCredit);
            userRepository.saveUser(user);
        }
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        // Retrieve context documents associated with the user and session
        List<Context> contexts = userService.getUserContextById(user, sessionId);

        return contexts;
    }

    // ... continue with other methods ...
}