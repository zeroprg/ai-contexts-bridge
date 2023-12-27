package com.bloberryconsulting.aicontextsbridge.config;


import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class HazelcastConfig {
    @Value("${hazelcast-client.cluster-name}")
    private String clusterName;

    @Value("${hazelcast-client.network-config.addresses}")
    private String addresses;

    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName(clusterName);
        config.getNetworkConfig().setPublicAddress(addresses);
        // Configure Hazelcast as needed
        return Hazelcast.newHazelcastInstance(config);

    }
}