package com.neusoft.neusoft_project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.entity.Appointment;
import com.neusoft.neusoft_project.entity.PatientProfiles;
import com.neusoft.neusoft_project.entity.Users;
import com.neusoft.neusoft_project.service.IAppointmentService;
import com.neusoft.neusoft_project.service.IUsersService;
import com.neusoft.neusoft_project.service.PatientProfilesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@CrossOrigin(origins = "http://localhost:5173") // Allows Vue connection
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private PatientProfilesService patientProfilesService; // Service to sync patient data

    @Autowired
    private IUsersService userService; // Service to fetch doctors

    // --- 1. NEW: Get Verified Doctors (For the Booking Dropdown) ---
    @GetMapping("/verified-doctors")
    public List<Users> getVerifiedDoctors() {
        QueryWrapper<Users> queryWrapper = new QueryWrapper<>();
        // Filter: Must be a DOCTOR and must be VERIFIED (approved by Admin)
        queryWrapper.eq("role", "DOCTOR")
                .eq("verified", true);

        // Optimization: Only fetch necessary fields (hide passwords)
        queryWrapper.select("id", "username", "specialization", "license_number");

        return userService.list(queryWrapper);
    }

    // --- 2. UPDATED: Book Appointment & Sync Patient Profile ---
    @PostMapping("/book")
    public boolean bookAppointment(@RequestBody Appointment appointment) {
        // A. Set Appointment Defaults
        appointment.setStatus("PENDING");
        if (appointment.getPriority() == null) {
            appointment.setPriority("ROUTINE");
        }

        // B. Save the Appointment to 'appointments' table
        boolean aptSaved = appointmentService.save(appointment);

        // C. SYNC: Save Patient Info to 'patient_profiles' table
        if (aptSaved && appointment.getPatientId() != null) {
            try {
                // 1. Check if profile exists
                PatientProfiles profile = patientProfilesService.getById(appointment.getPatientId());

                // 2. If not, create a new one
                if (profile == null) {
                    profile = new PatientProfiles();
                    profile.setUserId(appointment.getPatientId());
                }

                // 3. Update fields if provided in the booking form
                if (appointment.getDicomPatientId() != null) {
                    profile.setDicomPatientId(appointment.getDicomPatientId());
                }
                if (appointment.getPatientGender() != null) {
                    profile.setGender(appointment.getPatientGender());
                }
                if (appointment.getPatientBirthDate() != null) {
                    profile.setDateOfBirth(appointment.getPatientBirthDate());
                }
                if (appointment.getPatientName() != null) {
                    profile.setFullName(appointment.getPatientName());
                }
                if (appointment.getMetaData() != null) {
                    profile.setMetaData(appointment.getMetaData());
                }

                // 4. Save to database
                patientProfilesService.saveOrUpdate(profile);

            } catch (Exception e) {
                // Log error but don't fail the appointment booking
                System.err.println("Error syncing patient profile: " + e.getMessage());
            }
        }

        return aptSaved;
    }

    // --- 3. Get ALL Appointments ---
    @GetMapping("/all")
    public List<Appointment> getAllAppointments() {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("appointment_date");
        return appointmentService.list(queryWrapper);
    }

    // --- 4. Get by Patient ---
    @GetMapping("/patient/{patientId}")
    public List<Appointment> getPatientAppointments(@PathVariable Long patientId) {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("patient_id", patientId).orderByDesc("appointment_date");
        return appointmentService.list(queryWrapper);
    }

    // --- 5. Get by Doctor ---
    @GetMapping("/doctor/{doctorId}")
    public List<Appointment> getDoctorAppointments(@PathVariable Long doctorId) {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("doctor_id", doctorId).orderByDesc("appointment_date");
        return appointmentService.list(queryWrapper);
    }

    // --- 6. General Status Update ---
    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(newStatus);
        return appointmentService.updateById(appointment);
    }

    // --- 7. Complete Scan (Nurse Bridge) ---
    @PutMapping("/{id}/complete-scan")
    public ResponseEntity<?> completeScan(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Appointment apt = appointmentService.getById(id);

        if (apt == null) {
            return ResponseEntity.notFound().build();
        }

        // Update Status
        apt.setStatus("SCANNED");

        // Save Filename
        if (payload.containsKey("scanFile")) {
            apt.setScanFile(payload.get("scanFile"));
        }

        // Save Nurse Notes
        if (payload.containsKey("nurseNotes")) {
            apt.setNurseNotes(payload.get("nurseNotes"));
        }

        boolean success = appointmentService.updateById(apt);

        if (success) {
            return ResponseEntity.ok("Scan linked and status updated to SCANNED");
        } else {
            return ResponseEntity.status(500).body("Failed to update appointment");
        }
    }

    // --- 8. Get Scanned Appointments ---
    @GetMapping("/scanned")
    public List<Appointment> getScannedAppointments() {
        QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", "SCANNED").orderByDesc("appointment_date");
        return appointmentService.list(queryWrapper);
    }
}