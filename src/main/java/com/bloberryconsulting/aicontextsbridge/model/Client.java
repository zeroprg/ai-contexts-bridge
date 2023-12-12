package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client implements Serializable {
    private String Id;
    private String name;
    private String description; // This can be a JSON string or any format you prefer
    private String ownerId; // userId or customerId who own this profile or created
    // Add other fields as necessary
}
