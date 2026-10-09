package com.neusoft.neusoft_project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.entity.MedicalReport;
import com.neusoft.neusoft_project.service.IMedicalReportService;
import com.neusoft.neusoft_project.service.IAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reports")
// FIX: Allow BOTH localhost and 127.0.0.1 — browsers treat them differently
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class MedicalReportController {

    @Autowired
    private IMedicalReportService reportService;

    @Autowired
    private IAppointmentService appointmentService;

    // ─────────────────────────────────────────────────────────────────
    // POST /reports/save
    // Frontend ReportSystem.vue calls this when doctor clicks "Finalize"
    // Handles both INSERT (id=null) and UPDATE (id exists)
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/save")
    public ResponseEntity<MedicalReport> saveReport(@RequestBody MedicalReport report) {
        try {
            if (report.getReportDate() == null) {
                report.setReportDate(LocalDateTime.now());
            }
            // saveOrUpdate: INSERT when id is null, UPDATE when id has value
            reportService.saveOrUpdate(report);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /reports/appointment/{appointmentId}
    // PatientDashboard Appointments tab → "View Report" button
    // DoctorDashboard → checking if a report already exists for an apt
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getByAppointment(@PathVariable Long appointmentId) {
        QueryWrapper<MedicalReport> qw = new QueryWrapper<>();
        qw.eq("appointment_id", appointmentId);
        qw.orderByDesc("report_date");
        qw.last("LIMIT 1");
        MedicalReport report = reportService.getOne(qw);
        if (report == null) {
            return ResponseEntity.status(404).body("No report found for this appointment.");
        }
        return ResponseEntity.ok(report);
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /reports/patient/{patientId}
    // PatientDashboard "My Reports" tab — shows all reports for patient
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalReport>> getPatientReports(@PathVariable Long patientId) {
        QueryWrapper<MedicalReport> qw = new QueryWrapper<>();
        qw.eq("patient_id", patientId);
        qw.orderByDesc("report_date");
        return ResponseEntity.ok(reportService.list(qw));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /reports/doctor/{doctorId}
    // DoctorDashboard "Reports" tab — shows all reports written by doctor
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MedicalReport>> getDoctorReports(@PathVariable Long doctorId) {
        QueryWrapper<MedicalReport> qw = new QueryWrapper<>();
        qw.eq("doctor_id", doctorId);
        qw.orderByDesc("report_date");
        return ResponseEntity.ok(reportService.list(qw));
    }

    // ─────────────────────────────────────────────────────────────────
    // GET /reports/all  (admin / fallback)
    // ─────────────────────────────────────────────────────────────────
    @GetMapping("/all")
    public ResponseEntity<List<MedicalReport>> getAllReports() {
        QueryWrapper<MedicalReport> qw = new QueryWrapper<>();
        qw.orderByDesc("report_date");
        return ResponseEntity.ok(reportService.list(qw));
    }
}