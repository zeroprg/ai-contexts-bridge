package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role implements Serializable {
    String role;
    String description;    
}
