package com.bloberryconsulting.aicontextsbridge.service;

import com.bloberryconsulting.aicontextsbridge.model.User;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.concurrent.ConcurrentMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HazelcastService {
  
    private final HazelcastInstance hazelcastInstance;
    //private final Logger logger = LoggerFactory.getLogger(HazelcastService.class);


    public HazelcastService(HazelcastInstance hazelcastInstance, 
        @Value("${hazelcast-client.clear-maps}") boolean clearMaps,
        @Value("${hazelcast-client.backup-directory}")  String backupDirectory,
        @Value("${hazelcast-client.restore-maps}") boolean restoreMaps,
        @Value("${hazelcast-client.store-maps}") boolean storeMaps) 
        throws ClassNotFoundException, IOException {
        this.hazelcastInstance = hazelcastInstance;
        if (clearMaps) {
            clearAllMaps();
        }
        if (restoreMaps) {
            restoreMapsFromDirectory(backupDirectory);
        }else if (storeMaps) {
            storeMapsToDirectory(backupDirectory);
        }
    }
/*
    public void storeUser(IMap<String, User> userMap, User user) {
        try {

                    // Clone the user object
            User userToStore = user.clone();
            // Store each context separately
             // Create a new map for the modified contexts
            Map<String, Context> modifiedContexts = new HashMap<>();

            if (userToStore.getContexts() != null) {
                userToStore.getContexts().forEach((key, context) -> {
                    
                    // Clone the context and set the conversation history to null
                    Context contextToStore = new Context(context);
                    contextToStore.setConversationHistory(null);
                    modifiedContexts.put(key, contextToStore);
    
                    // Store the original context separately
                    storeContext(context);
                });
                // Set the modified contexts map to the cloned user , Do aka deep copy
                userToStore.setContexts(modifiedContexts);

            }

            // Store the user object
            userMap.put(userToStore.getId(), userToStore);

            logger.debug("User '{}' stored in Hazelcast", userToStore.getId());
        } catch (Exception e) {
            logger.error("Error storing user in Hazelcast: {}", e.getMessage());
        }
    }


    public User restoreUser(IMap<String, User> userMap, String userId) {
        User user = userMap.get(userId);
        if (user != null && user.getContexts() != null) {
            // Retrieve the conversation history for each context
                user.getContexts().forEach((key, context) -> {
                    JSONArray conversationHistory = retrieveConversationHistory(context.getSessionId());
                    context.setConversationHistory(conversationHistory);
                });
   
               
        }

        return user;
    }

     
    public void storeContext(Context context) {
        if(context.getConversationHistory() == null) return;
        try {
            // Serialize the conversation history to a byte array
            byte[] conversationHistoryBytes = serializeToJsonByteArray(context.getConversationHistory());
            
            // store the history of conversation in the hazelcast map
           
            hazelcastInstance.getMap(CONTEXT_MAP).put(context.getSessionId() + "_history", conversationHistoryBytes);
            logger.info("Context '{}' updated in Hazelcast", context.getSessionId());
        } catch (IOException e) {
            logger.error("Error storing context in Hazelcast: {}", e.getMessage());
        }
    }

    public JsonArray retrieveConversationHistory(String sessionId) {
        IMap<String, Object> map = hazelcastInstance.getMap(CONTEXT_MAP);       
        byte[] conversationHistoryBytes = (byte[]) map.get(sessionId + "_history");
        JSONArray conversationHistory = null;
        if (conversationHistoryBytes != null) {
            try {
                conversationHistory = deserializeJsonByteArray(conversationHistoryBytes);
                
            } catch (Exception e) {
                logger.error("Error deserializing conversation history for context '{}': {}", sessionId, e.getMessage());
            }
        }

        return conversationHistory;
    }

    private byte[] serializeToJsonByteArray(JSONArray jsonArray) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(jsonArray.toString());
            return bos.toByteArray();
        }
    }

    private JsonArray deserializeJsonByteArray(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis)) {
            String jsonArrayString = (String) in.readObject();
            return new JSONArray(jsonArrayString);
        }
    }
*/

    public Collection<String> retrieveAllMapsIds(String mapName) {
        IMap<String, User> userMap = hazelcastInstance.getMap(mapName);
        return userMap.keySet();
    }

    public IMap<String, ?> getMap(String mapName) {
        IMap<String, ?> map = hazelcastInstance.getMap(mapName);
        return map;
    }

    

    // Method to clear all maps in the Hazelcast instance
    public void clearAllMaps() {
        Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();

        for (DistributedObject distributedObject : distributedObjects) {
            if (distributedObject instanceof IMap) {
                ConcurrentMap<?, ?> map = (IMap<?, ?>) distributedObject;
                map.clear();
            }
        }
    }

    // Method to store all maps to a directory
    public void storeMapsToDirectory(String directoryPath) throws IOException {
        Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();

        for (DistributedObject distributedObject : distributedObjects) {
            if (distributedObject instanceof IMap) {
                IMap<?, ?> map = (IMap<?, ?>) distributedObject;
                String mapName = map.getName();
                File mapFile = new File(directoryPath, mapName + ".ser");

                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(mapFile))) {
                    oos.writeObject(map);
                }
            }
        }
    }

    // Method to restore all maps from a directory
    public void restoreMapsFromDirectory(String directoryPath) throws IOException, ClassNotFoundException {
        File[] mapFiles = new File(directoryPath).listFiles((dir, name) -> name.endsWith(".ser"));

        if (mapFiles != null) {
            for (File mapFile : mapFiles) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mapFile))) {
                    IMap<?, ?> map = (IMap<?, ?>) ois.readObject();
                    String mapName = map.getName();
                    hazelcastInstance.getMap(mapName).clear();
                    hazelcastInstance.getMap(mapName).putAll(map);
                }
            }
        }
    }


    /**
     * Retrieves all data from the specified Hazelcast map.
     * @param <T>
     * @param mapName
     * @return
     */
    public <T> Collection<T> retrieveAll(String mapName) {
        ConcurrentMap<String, T> map = hazelcastInstance.getMap(mapName);
        return map.values();
    }


    /**
     * Stores data in the specified Hazelcast map.
     * 
     * @param mapName The name of the Hazelcast map.
     * @param key The key under which the data is to be stored.
     * @param value The data to be stored.
     */
    public void storeData(String mapName, String key, Object value) {
        ConcurrentMap<String, Object> map = hazelcastInstance.getMap(mapName);
        map.put(key, value);

    }

    /**
     * Retrieves data from the specified Hazelcast map.
     * 
     * @param mapName The name of the Hazelcast map.
     * @param key The key of the data to be retrieved.
     * @return The data associated with the given key, or null if not found.
     */
    public Object retrieveData(String mapName, String key) {
        ConcurrentMap<String, Object> map = hazelcastInstance.getMap(mapName);
        return map.get(key);
    }

}