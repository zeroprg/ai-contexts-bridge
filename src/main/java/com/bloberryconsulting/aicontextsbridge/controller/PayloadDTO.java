package com.bloberryconsulting.aicontextsbridge.controller;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayloadDTO implements Serializable{
    private String data;
    private String sessionId;
}