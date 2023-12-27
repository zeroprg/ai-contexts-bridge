package com.bloberryconsulting.aicontextsbridge.model;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class Context implements Serializable{
    private String sessionId; // Session ID play as id for context
    private String name; // Context's name
    private Date lastUsed; // Last used date
    private String userId; // userId or  clientId link to client/user (company profile) ( many Users to one Client)
    private String[] documents; // Context's documents
}
