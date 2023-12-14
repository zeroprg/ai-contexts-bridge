package com.bloberryconsulting.aicontextsbridge.repository;

import com.bloberryconsulting.aicontextsbridge.model.ApiKey;
import com.bloberryconsulting.aicontextsbridge.model.Bill;
import com.bloberryconsulting.aicontextsbridge.model.Client;
import com.bloberryconsulting.aicontextsbridge.model.ProfileDetails;
import com.bloberryconsulting.aicontextsbridge.model.User;
import com.bloberryconsulting.aicontextsbridge.service.HazelcastService;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private static final String CUSTOMER_MAP = "user";
    private static final String API_KEYS_MAP = "apiKey";
    private static final String PROFILES_MAP = "profile";
    private static final String CLIENT_MAP = "client";
    private static final String BILL_MAP = "bill";

    private final HazelcastService hazelcastService;


    public UserRepository(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    /**
     * Creates a new client profile in the repository.
     *
     * @param profileDetails The client profile to create.
     */
    public void createProfile(ProfileDetails profileDetails) {
        hazelcastService.storeData(PROFILES_MAP, profileDetails.getId(), profileDetails);
    }

    /**
     * Updates an existing client profile in the repository.
     *
     * @param profileDetails The client profile with updated information.
     */
    public void updateProfile(String profileId, ProfileDetails profileDetails) {
        hazelcastService.storeData(PROFILES_MAP, profileId, profileDetails);
    }

    /**
     * Finds a client profile by its ID.
     *
     * @param profileId The ID of the client whose profile is to be retrieved.
     * @return The client profile if found, or null otherwise.
     */
    public ProfileDetails getProfileById(String profileId) {
        return (ProfileDetails) hazelcastService.retrieveData(PROFILES_MAP, profileId);
    }


    /**
     * Retrieves all  profiles stored in the repository.
     *
     * @return A collection of all  profiles.
     */
    public Collection<ProfileDetails> findAllProfiles() {
        return hazelcastService.retrieveAll(PROFILES_MAP);
    }


    /**
     * Retrieves all client profiles stored in the repository.
     *
     * @return A collection of all client profiles.
     */
    public Collection<ProfileDetails> findAllClientProfiles() {
        return hazelcastService.retrieveAll(CLIENT_MAP);
    }


    /**
     * Saves an API key in the repository.
     *
     * @param apiKey The API key to be saved.
     */
    public void saveApiKey(ApiKey apiKey) {
        hazelcastService.storeData(API_KEYS_MAP, apiKey.getKeyId(), apiKey);
    }

    /**
     * Updates an existing API key in the repository.
     *
     * @param apiKey The API key with updated information.
     */
    public void updateApiKey(ApiKey apiKey) {
        hazelcastService.storeData(API_KEYS_MAP, apiKey.getKeyId(), apiKey);
    }

    /**
     * Finds an API key by its ID.
     *
     * @param apiKeyId The ID of the API key to find.
     * @return The found API key, or null if not found.
     */
    public ApiKey findApiKeyByApiKeyId(String apiKey) {
        return (ApiKey) hazelcastService.retrieveData(API_KEYS_MAP, apiKey);
    }   

    /**
     * Retrieves all API keys stored in the repository.
     *
     * @return A collection of all API keys.
     */
    public Collection<ApiKey> findApiKeysByUserId(String userId) {
      
        Collection<ApiKey> keys = hazelcastService.retrieveAll(API_KEYS_MAP);
    return keys.stream()
                .filter(apiKey -> userId.equals(apiKey.getUserId()) || apiKey.isPublicAccessed())
                .collect(Collectors.toList());
    }

    // Profile Details specific methods

    /** 
     * Retrieves all profiles stored in the repository.
     */
    private Collection<ProfileDetails> getAllDetailProfiles() {        
        return hazelcastService.retrieveAll(CUSTOMER_MAP);
    }

    /**
     * Finds a profile by the user (customer) ID.
     * 
     * @param userId The ID of the user whose  profile is to be retrieved.
     * @return The profile associated with the given user ID, or null if not found.
     */
    public Collection<ProfileDetails> findProfileByCustomerId(String userId) {
        return getAllDetailProfiles().stream().filter(profile -> userId.equals(profile.getOwnerId()))
                    .collect(Collectors.toList());
    }


    /**
     * Finds a client profile by the client ID.
     * @param clientId
     * @return
     */
    public Collection<ProfileDetails> findProfileByClientId(String clientId) {
        return getAllDetailProfiles().stream().filter(profile -> clientId.equals(profile.getOwnerId()))
                    .collect(Collectors.toList());
    }

    /**
     * Saves or updates a client profile in the repository.
     * 
     * @param clientId The ID of the client whose profile is to be saved or updated.
     * @param profileDetails The profile details to be saved or updated.
     */
    public void saveOrUpdateClientProfile(String clientId, ProfileDetails profileDetails) {
        hazelcastService.storeData(PROFILES_MAP, clientId, profileDetails);
    }

    // Client specific methods

        /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user to find.
     * @return The found user, or null if not found.
     */
    public Client findClientById(String clientId) {
        return (Client) hazelcastService.retrieveData(CUSTOMER_MAP,clientId);
    }

    /** 
     * Retrieves all clients stored in the repository.
     */
    public Collection<Client> getAllClients() {        
        return hazelcastService.retrieveAll(CLIENT_MAP);
    }

    /**
     * Saves or updates a client profile in the repository.
     * 
     * @param clientId The ID of the client whose profile is to be saved or updated.
     * @param profileDetails The profile details to be saved or updated.
     */
    public void saveOrUpdateClient(String clientId, Client client) {
        hazelcastService.storeData(PROFILES_MAP, clientId, client);
    }

    // User's specific methods

    /**
     * Finds a user by their ID.
     *
     * @param userId The ID of the user to find.
     * @return The found user, or null if not found.
     */
    public User findUserById(String userId) {
        return (User) hazelcastService.retrieveData(CUSTOMER_MAP,userId);
    }


    /**
     * Creates a new user in the repository.
     *
     * @param user The user to create.
     */
    public void createUser(User user) {
        hazelcastService.storeData(CUSTOMER_MAP,user.getId(), user);
    }

    /**
     * Updates an existing user in the repository.
     *
     * @param user The user with updated information.
     */
    public void updateUser(User user) {
        hazelcastService.storeData(CUSTOMER_MAP, user.getId(), user);
    }


    /** 
     * Retrieves all users stored in the repository.
     */
    public Collection<User> getAllUsers(String userId) {        
        return hazelcastService.retrieveAll(CUSTOMER_MAP);
    }


    /**
     * Finds a user by their email.
     *
     * @param email The email of the user to find.
     * @return The found user, or null if not found.
     */
    public User findUserByEmail(String email) {
        Collection<User> users = hazelcastService.retrieveAll(CUSTOMER_MAP);
        return users.stream()
                    .filter(user -> email.equals(user.getEmail())).findFirst().orElse(null);

    }

    /**
     * Finds a user by their name.
     *
     * @param name The name of the user to find.
     * @return The found user, or null if not found.
     */
    public User findUserByName(String name) {
        Collection<User> users = hazelcastService.retrieveAll(CUSTOMER_MAP);
        return users.stream()
                    .filter(user -> name.equals(user.getName()))
                    .findFirst()
                    .orElse(null);
    }

    /**
     * Assigns a new role to a user.
     *
     * @param userId The ID of the user to whom the role is to be assigned.
     * @param role The role to be assigned to the user.
     */
    public void assignRoleToUser(String userId, String role) {
        User user = findUserById(userId);
        if (user != null) {
            user.getRoles().add(role);
            updateUser(user);
        }
    }
   
    /**
     * Saves a collection of users in the repository.
     * @param users
     */
   public void saveAllUsers(Collection<User> users) {
        users.forEach(user -> hazelcastService.storeData(CUSTOMER_MAP, user.getId(), user));
    }

    /**
     * Find a user's bill by their user ID.
     *
     * @param userId The ID of the user's bill to find.
     * @return The found user, or null if not found.
     */
    public Bill findBillByUserId(String userId) {
        return (Bill) hazelcastService.retrieveData(BILL_MAP,userId);
    }
    

    /**
     * Finds a Bill by their name.
     *
     * @param billId The ID of the Bill to find.
     * @return The found Bill, or an empty Optional if not found.
     */
    public Bill findBillById(String billId) {
        return getAllBills().stream()
                .filter(bill -> billId.equals(bill.getBillId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates a new Bill in the repository.
     *
     * @param bill The Bill to create.
     */
    public void createBill(Bill bill) {
        hazelcastService.storeData(BILL_MAP, bill.getBillId(), bill);
    }

    /**
     * Updates an existing Bill in the repository.
     *
     * @param bill The Bill with updated information.
     */
    public void updateBill(Bill bill) {
        hazelcastService.storeData(BILL_MAP, bill.getBillId(), bill);
    }

    /**
     * Retrieves all Bills stored in the repository.
     *
     * @return A collection of all Bills.
     */
    public Collection<Bill> getAllBills() {
        return hazelcastService.retrieveAll(BILL_MAP);
    }


     // Other private methods...
 
}
