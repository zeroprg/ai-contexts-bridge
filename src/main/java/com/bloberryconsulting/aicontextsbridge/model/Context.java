package com.bloberryconsulting.aicontextsbridge.model;

import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor


public class Context implements Serializable, Cloneable {
    private String sessionId;
    private String name;
    private Date lastUsed;
    private String userId;
    private String[] documents;
    private String conversationHistory; // serialized JSONArray
    private String assistantRoleMessage;

    // Existing constructors, getters, setters, and other methods...

    // Copy constructor
    public Context(Context other) {
        this.sessionId = other.sessionId;
        this.name = other.name;
        this.lastUsed = (other.lastUsed != null) ? (Date) other.lastUsed.clone() : null;
        this.userId = other.userId;
        this.documents = (other.documents != null) ? other.documents.clone() : null;
        // For JSONArray, we just copy the reference. Deep copy is tricky and depends on your needs.
        this.conversationHistory = other.conversationHistory;
        this.assistantRoleMessage = other.assistantRoleMessage;
    }
}
