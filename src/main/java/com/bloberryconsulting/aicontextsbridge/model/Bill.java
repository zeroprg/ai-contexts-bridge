package com.bloberryconsulting.aicontextsbridge.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill implements Serializable {
    private String userId;
    private double totalCost;
    private double totalTax = 0.05;
}    