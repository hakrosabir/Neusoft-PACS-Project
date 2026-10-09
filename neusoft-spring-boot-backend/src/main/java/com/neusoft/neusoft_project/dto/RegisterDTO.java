package com.neusoft.neusoft_project.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    // Basic User Info
    private String username;
    private String password;
    private String role; // Will be "DOCTOR" or "PATIENT"

    // Doctor Specific Info
    private String specialization;
    private String licenseNumber;

    // Patient Specific Info
    private String insuranceId;
    private String emergencyContact;
}