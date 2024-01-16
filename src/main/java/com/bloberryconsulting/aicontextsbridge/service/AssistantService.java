package com.bloberryconsulting.aicontextsbridge.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.bloberryconsulting.aicontextsbridge.model.Context;
import com.bloberryconsulting.aicontextsbridge.model.Role;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;



@Service
public class AssistantService {
    private final UserService userService;
    private final HazelcastInstance hazelcastInstance;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantService(UserService userService, HazelcastInstance hazelcastInstance, ResourceLoader resourceLoader) {
        this.userService = userService;
        this.hazelcastInstance = hazelcastInstance;
        this.resourceLoader = resourceLoader;   
        loadRolesIntoHazelcast();   // Load roles into Hazelcast on startup
    }

    /**
     * Retrieves all available assistance roles for a specific user.
     * @param userId The ID of the user.
     * @return An array of assistance roles.
     */
    public String[] getAllAssistanceRoles(User user) {
        // Implementation logic to get all assistance roles for the specific user
        // This could involve querying a database or an external service
        return user.getContexts().entrySet().stream()
                .map(entry -> entry.getValue().getAssistantRoleMessage())
                .toArray(String[]::new);
    }

    /**
     * Retrieves all available assistance roles from Hazelcast.
     * @return An array of assistance roles.
     */
    public Role[] getAllAssistanceRoles() {
        // Assuming roles are stored in a Hazelcast map with a specific key
        IMap<String, Role[]> rolesMap = hazelcastInstance.getMap("rolesMap");
        Role[] roles = rolesMap.get("assistantRoles");

        // Check if roles are found, otherwise return an empty array
        return roles != null ? roles : new Role[]{};
    }

    private void loadRolesIntoHazelcast() {
        try {
            // Load and parse the JSON file
            
            Resource resource = resourceLoader.getResource("classpath:assistant-roles.json");
            if (!resource.exists()) {
                throw new FileNotFoundException("users.json file not found in classpath");
            }

            InputStream inputStream = resource.getInputStream();
            Role[] roles = objectMapper.readValue(inputStream, new TypeReference<Role[]>(){});

            // Store the roles in Hazelcast
            IMap<String, Role[]> rolesMap = hazelcastInstance.getMap("rolesMap");
            rolesMap.put("assistantRoles", roles);

            System.out.println("Roles loaded into Hazelcast: " + Arrays.toString(roles));
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception (e.g., file not found, parsing error)
        }
    }

    /**
     * Retrieves all available assistance roles for a specific user and context.
     * @param userId The ID of the user.
     * @param contextId The ID of the context.
     * @return An array of assistance roles.
     */
    public Role[] getAssistanceRoles(User user, String sessionId) {
        if (user.getContexts() == null) {
            return null;
        }
    
        // Get all assistance roles
        Role[] allRoles = this.getAllAssistanceRoles();  

        return Arrays.stream(allRoles)
                .filter(role -> user.getContexts().entrySet().stream()
                    .filter(entry -> entry.getValue().getSessionId().equals(sessionId))
                    .anyMatch(entry -> role.getRole().equals(entry.getValue().getAssistantRoleMessage()))
                )
                .toArray(Role[]::new); // Use a generator function to create a Role[] array

    }
    

    /**
     * Sets the assistance role message for a specific user and context.
     * @param user The user object.
     * @param contextId The ID of the context.
     * @param assistanceRoleMessage The assistance role message to set.
     */
    public void setAssistanceRole(User user, String contextId, String assistanceRoleMessage) {
        List<Context> userContexts = userService.getUserContextById(user, contextId) ;
        Context userContext = null;
        if (userContexts != null &&  userContexts.size()>0) { 
            userContext  =  userContexts.get(0);
            userContext.setAssistantRoleMessage(assistanceRoleMessage);
        } else {
            userContext = new Context();
            userContext.setAssistantRoleMessage(assistanceRoleMessage);
            userContext.setSessionId(contextId);
            userContext.setUserId(user.getId());
            userContext.setName("Role set to: '"+ assistanceRoleMessage+"'");
        }
        userContext.setLastUsed(new Date());
        // Additional logic might be needed, such as saving the updated user context
        userService.updateUsersContexts(user, userContext);
    }
}

