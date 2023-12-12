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
    private Double totalCost; //Total charged for this api key
}