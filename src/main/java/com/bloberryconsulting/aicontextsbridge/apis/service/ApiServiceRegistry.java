package com.bloberryconsulting.aicontextsbridge.apis.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApiServiceRegistry implements InitializingBean {

    private final ApplicationContext applicationContext;
    private final Map<String, ApiService> serviceMap = new HashMap<>();

    public ApiServiceRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        // Populate the service map
        Map<String, ApiService> apiServices = applicationContext.getBeansOfType(ApiService.class);
        for (ApiService service : apiServices.values()) {
            String apiId = service.getApiId();
            serviceMap.put(apiId, service);
        }
    }

    public ApiService getService(String APIId) {
        ApiService service = serviceMap.get(APIId);
        if (service == null) {
            throw new IllegalArgumentException("Invalid APIId: " + APIId);
        }
        return service;
    }
}
