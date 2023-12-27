package com.bloberryconsulting.aicontextsbridge.service;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

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


    // Method to clear all maps in the Hazelcast instance
    public void clearAllMaps() {
        Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();

        for (DistributedObject distributedObject : distributedObjects) {
            if (distributedObject instanceof IMap) {
                IMap<?, ?> map = (IMap<?, ?>) distributedObject;
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