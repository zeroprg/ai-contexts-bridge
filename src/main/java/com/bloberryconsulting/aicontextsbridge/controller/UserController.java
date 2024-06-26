package com.bloberryconsulting.aicontextsbridge.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bloberryconsulting.aicontextsbridge.exceptions.APIError;
import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.model.Role;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.service.AssistantService;
import com.bloberryconsulting.aicontextsbridge.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;


import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_CLIENT_ADMINISTRATOR_DESC;
import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_SITE_ADMINISTRATOR_DESCR;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

//import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_CUSTOMER_DESCR;
import static com.bloberryconsulting.aicontextsbridge.security.SecurityConfiguration.ROLE_APIKEY_MANAGER_DESC;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "${ui.uri}", allowCredentials = "true")
public class UserController {
    private final UserService userService;
    private final AssistantService assistanceService;

    public UserController(UserService userService, AssistantService assistanceService) {
        this.userService = userService;
        this.assistanceService = assistanceService;
    }

    @Operation(
        summary = "Get User Information",
        description = "Retrieves information about the currently authenticated user.",
        security = @SecurityRequirement(name = "oauth2scheme"),
       tags = {"User's CRUD operations"}
    ) 
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieves curent user info",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })    
    @GetMapping("/info")
    public ResponseEntity<User> getUserInfo() {
        OAuth2User principal = (OAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

         // Access user details

        String userId = principal.getAttribute("sub"); // 'sub' is typically the user ID in OAuth 2.0
    
        // Extract additional attributes as needed
        // The keys for these attributes depend on the OAuth provider and the scopes granted
        // Example: String locale = principal.getAttribute("locale");
        // Example: String phoneNumber = principal.getAttribute("phone_number");
    
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(
    summary = "Select API for current user by APIKey id",
    description = "Assign a new API to the currently authenticated user. No roles required.",
    security = @SecurityRequirement(name = "oauth2scheme"),
         tags = {"Customer API Query"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updates user with selected API",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/selectAPI/{apiKey}")
    public ResponseEntity<String> selectAPI(@PathVariable  String apiKey) {
        OAuth2User principal = (OAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.NON_AUTHORITATIVE_INFORMATION).body("No authenticated user");
        }
          
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName(); // Assuming this retrieves the user ID

        // Access user details
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        userService.updateUserAPI(currentUserId, apiKey);
        return ResponseEntity.ok("User Name: " + name + ", Email: " + email + " was selected API: " + apiKey);
    }


    @PreAuthorize("hasAuthority('ROLE_CLIENT_ADMINISTRATOR')")
    @Operation(
    summary = "Attach user (customer) to the client(company).",
    description = "After attachment user will be associated with client and can use all clients APIs."                
                 + ROLE_CLIENT_ADMINISTRATOR_DESC,
    security = @SecurityRequirement(name = "oauth2scheme"),
         tags = {"User's CRUD operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully attach user with client",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{userId}/attach/{clientId}")
    public ResponseEntity<String> attachUserToClient(@PathVariable String userId, @PathVariable String clientId ) { 

        User user = userService.getUserById(userId);
        user.setClientId(clientId);
        return ResponseEntity.ok("User Name: "+ user.getName() + ", Email: " + user.getEmail() + " was attached to client: " + clientId);
    }


    @PreAuthorize("hasAuthority('ROLE_SITE_ADMINISTRATOR')")
    @Operation(
        summary = "Find User by eMail",
        description = "Finds a user in the system by their email address." + 
                         ROLE_SITE_ADMINISTRATOR_DESCR,
        security = @SecurityRequirement(name = "oauth2scheme"),
        tags = {"User's CRUD operations"}
    ) 
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found user by eMail",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/findByEmail")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email) {
        User user = userService.findUserByEmail(email);
        return ResponseEntity.ok(user);
    }


    @PreAuthorize("hasAuthority('ROLE_SITE_ADMINISTRATOR')")
    @Operation(
        summary = "Find user by name",
        description = "Finds a user in the system by full name." +
                       ROLE_SITE_ADMINISTRATOR_DESCR,
        security = @SecurityRequirement(name = "oauth2scheme"),
        tags = {"User's CRUD operations"}
    ) 
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully found user by name",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })    
    @GetMapping("/findByName")
    public ResponseEntity<User> getUserByName(@RequestParam String name) {
        User user = userService.findUserByName(name);
        return ResponseEntity.ok(user);
    }

    @PreAuthorize("hasAuthority('ROLE_APIKEY_MANAGER')")
    @Operation(summary = "Assigne user a new role"
        , description = "Assigns a new role to a user. required super admin role" +
          ROLE_APIKEY_MANAGER_DESC,
         security = @SecurityRequirement(name = "oauth2scheme"),
        tags = {"User's CRUD operations"}
    ) 
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully assigne user a new role",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),        
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    }) 
    @PostMapping("/assignRole")
    public ResponseEntity<String> assignRoleToUser(@RequestParam String email, @RequestParam String role) {
        userService.assignRoleToUser(email, role);
        return ResponseEntity.ok("Role assigned successfully");
    }



    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully stored documents",
                     content = @Content(mediaType = "application/json", 
                                        schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request - Invalid documents provided",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/context/store")
    public ResponseEntity<?> storeContext(
                HttpServletRequest request,
                @Parameter(description = "Context to be stored")
                @RequestBody Context context) {
        // Getting the session ID from the HttpServletRequest
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        User user = this.getUserInfo().getBody();
        context.setLastUsed(new java.util.Date());
        context.setSessionId(sessionId);
        context.setUserId(user.getId());    
        userService.updateUsersContexts(user, context);    
        return ResponseEntity.ok(user);
    }
    
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Successfully deleted the file",
                 content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid file identifier provided",
                 content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "401", description = "User is not authenticated",
                 content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "403", description = "User is not authorized to delete this file",
                 content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "404", description = "File not found",
                 content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "500", description = "Internal Server Error",
                 content = @Content(mediaType = "application/json"))
})
@DeleteMapping("/context/deleteFile")
public ResponseEntity<User> deleteFile(
            HttpServletRequest request,
            @Parameter(description = "Identifier of the file (file name) to be deleted")
            @RequestParam String fileId) {        // Getting the session ID from the HttpServletRequest
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        User user = this.getUserInfo().getBody();
        boolean isDeleted = userService.deleteFile(user, sessionId, fileId);
        if(!isDeleted) {
           throw new APIError(HttpStatus.EXPECTATION_FAILED, "Couldn't find file to delete:"+fileId);
        }
        
        userService.updateUser(user);  
        return ResponseEntity.ok(user);
}

 
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved document's context",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = String[].class))),
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Documents not found",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/context/{sessionId}")
    public ResponseEntity<?> getContext(HttpServletRequest request, @PathVariable String sessionId) {
        User user = this.getUserInfo().getBody();
        // Assuming 'retrieveService' is a service that handles retrieval of documents
        List<Context> contexts = userService.getUserContextById(user, sessionId);
    
        if (contexts == null || contexts.size() == 0 ) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Context not found");
        }
    
        return ResponseEntity.ok(contexts.get(0));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all GPT assistant roles",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = String[].class))),
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/GPTAssistantRoles")
    public ResponseEntity<?> getAllGPTAssistanceRoles() {
        // Retrieve all assistant roles 
        Role[] roles = assistanceService.getAllAssistanceRoles();
        return ResponseEntity.ok(roles);
    }
    
    
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved GPT assistant role for specific sessionID",
                     content = @Content(mediaType = "application/json",
                                        schema = @Schema(implementation = String[].class))),
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to access this resource",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/GPTAssistantRole")
    public ResponseEntity<?> getGPTAssistanceRole(HttpServletRequest request) {
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        // Assuming the assistant roles are fetched based on the session ID
        User user = this.getUserInfo().getBody();
        Role[] roles  = assistanceService.getAssistanceRoles(user, sessionId);
        Role assistantRoleMessage = roles != null && roles.length>0 ? roles[0] : null;
        return ResponseEntity.ok(assistantRoleMessage);
    }

    @Operation(summary = "Set GPT Assistant Role Message",
               description = "Sets the role message for the GPT assistant.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully set the assistant role message",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Invalid role message provided",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "User is not authenticated",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "User is not authorized to perform this action",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal Server Error",
                     content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/setGPTAssistantRole")
    public ResponseEntity<?> setGPTAssistantRole(HttpServletRequest request,
        @Parameter(description = "Assistant role message to set")
        @RequestBody PayloadDTO payload) {
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : "No session";
        User user = this.getUserInfo().getBody();
        // Assuming the role message is processed and set here
        // You might need to add user and context information based on your requirements
        assistanceService.setAssistanceRole(user, sessionId, payload.getData());

        return ResponseEntity.ok("Assistant role message set successfully.");

        // Include other exception handling as needed
    }    
}
