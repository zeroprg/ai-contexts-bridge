package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey  implements Serializable {
    private String keyId;
    private String keyValue;
    private String name;
    private String uri; // Comma-separated roles
    private String homepage; // Unique identifier
    private String userId; // User's id , owner id
    private int maxContextLength; // Max context length
    private Double totalCost = 0.0; //Total charged for this api key
    private boolean publicAccessed; //Is it Public accessed
    private boolean defaultKey;
    private boolean disabled; //Is it disabled if true will be disabled and showen as aka default key
    private String model; // Model name
    private String description; // Description
}