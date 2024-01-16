package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable, Cloneable {
    private String id; // Unique identifier
    private String name; // User's name
    private String email; // User's email
    private String recentApiId; // ID of the most recently used API
    private Date lastLogin; // Last login date
    private String pictureLink; // link to picture icon
    private String clientId; // clientId link to client (company profile) ( many Users to one Client)
    private Map<String,Context> contexts; // User's contexts names <fileNAme>:<context>
    private HashSet<String> roles; // User's roles as strings
    private Double credit = 0.0; // User's credit

    @Override
    public User clone() {
        try {
            return (User) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // Can't happen
        }
    }
}
