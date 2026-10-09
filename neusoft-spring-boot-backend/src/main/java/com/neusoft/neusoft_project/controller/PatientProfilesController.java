package com.neusoft.neusoft_project.controller;

import com.neusoft.neusoft_project.entity.PatientProfiles;
import com.neusoft.neusoft_project.service.PatientProfilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * Frontend Controller for Patient Profiles
 * </p>
 *
 * @author 李如一 2428042
 * @since 2025-12-18
 */
@RestController
@RequestMapping("/patientProfiles")
@CrossOrigin(origins = "http://localhost:5173") // Allow Vue Frontend Access
public class PatientProfilesController {

    @Autowired
    PatientProfilesService patientProfilesService;

    // Existing method (Updated to return JSON list)
    @GetMapping("/showpatientinfo")
    public List<PatientProfiles> showpatientinfo(){
        return patientProfilesService.list();
    }

    // New specific endpoint for the Dropdown (cleaner name)
    @GetMapping("/list")
    public List<PatientProfiles> getPatientList() {
        return patientProfilesService.list();
    }
}