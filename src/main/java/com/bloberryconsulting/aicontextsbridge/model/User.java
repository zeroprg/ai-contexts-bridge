package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private String id; // Unique identifier
    private String name; // User's name
    private String email; // User's email
    private String recentApiId; // ID of the most recently used API
    private Date lastLogin; // Last login date
    private String pictureLink; // link to picture icon
    private String clientId; // clientId link to client (company profile) ( many Users to one Client)
    private HashSet<String> roles; // User's roles as strings
}
