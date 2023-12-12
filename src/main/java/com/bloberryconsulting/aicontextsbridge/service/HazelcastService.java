package com.bloberryconsulting.aicontextsbridge.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.Collection;

import org.springframework.stereotype.Service;

@Service
public class HazelcastService {

    private final HazelcastInstance hazelcastInstance;

    public HazelcastService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    /**
     * Retrieves all data from the specified Hazelcast map.
     * @param <T>
     * @param mapName
     * @return
     */
    public <T> Collection<T> retrieveAll(String mapName) {
        IMap<String, T> map = hazelcastInstance.getMap(mapName);
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
        IMap<String, Object> map = hazelcastInstance.getMap(mapName);
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
        IMap<String, Object> map = hazelcastInstance.getMap(mapName);
        return map.get(key);
    }

}