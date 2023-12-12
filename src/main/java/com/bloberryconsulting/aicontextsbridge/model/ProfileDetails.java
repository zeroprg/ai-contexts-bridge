package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 *  Context used to story some data related to user or client (company) between REST APIs calls
 */
public class ProfileDetails implements Serializable {
    private String Id;
    private String name;
    private String profileData; // This can be a JSON string or any format you prefer
    private String ownerId; // userId or clientId (company) who own this profile ( many profiles  to one Client)
    // Add other fields as necessary
}
