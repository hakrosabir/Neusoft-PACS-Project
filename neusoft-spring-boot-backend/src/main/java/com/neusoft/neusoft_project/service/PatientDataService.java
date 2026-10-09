package com.neusoft.neusoft_project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.entity.Appointment;
import com.neusoft.neusoft_project.entity.MedicalReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PatientDataService {

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private IMedicalReportService reportService;

    public String getPatientRecords(String patientIdStr) {
        StringBuilder context = new StringBuilder();
        Long patientId;

        try {
            patientId = Long.parseLong(patientIdStr);
        } catch (NumberFormatException e) {
            return "Error: Invalid Patient ID format.";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // ── SECTION 1: APPOINTMENTS ───────────────────────────────────────────
        QueryWrapper<Appointment> aptQuery = new QueryWrapper<>();
        aptQuery.eq("patient_id", patientId).orderByDesc("appointment_date");
        List<Appointment> appointments = appointmentService.list(aptQuery);

        context.append("=== APPOINTMENT HISTORY (").append(appointments.size()).append(" total) ===\n");
        if (appointments.isEmpty()) {
            context.append("No appointment history found.\n");
        } else {
            for (Appointment apt : appointments) {
                context.append(String.format(
                        "- Date: %s | Time: %s | Doctor: %s (%s) | Status: %s\n",
                        apt.getAppointmentDate(),
                        apt.getTimeSlot(),
                        apt.getDoctorName(),
                        apt.getDepartment(),
                        apt.getStatus()
                ));
            }
        }
        context.append("\n");

        // ── SECTION 2: MEDICAL REPORTS ────────────────────────────────────────
        QueryWrapper<MedicalReport> reportQuery = new QueryWrapper<>();
        reportQuery.eq("patient_id", patientId).orderByDesc("report_date");
        List<MedicalReport> reports = reportService.list(reportQuery);

        context.append("=== MEDICAL REPORTS & DIAGNOSES (").append(reports.size()).append(" total) ===\n");
        if (reports.isEmpty()) {
            context.append("No medical reports on file.\n");
        } else {
            for (MedicalReport rep : reports) {
                // IMPORTANT: Never append Base64 image fields (axialImage etc.) —
                // they are huge strings that will crash Ollama's context window.
                String dateStr = rep.getReportDate() != null
                        ? rep.getReportDate().format(formatter) : "N/A";

                context.append(String.format("--- Report ID: %s | Date: %s ---\n",
                        rep.getId(), dateStr));

                // Scan type and severity — critical for AI to answer "what kind of scan"
                context.append("   Scan Type: ").append(typeLabel(rep.getAnalysisType())).append("\n");
                context.append("   Severity: ").append(capitalize(rep.getSeverity())).append("\n");
                context.append("   Doctor: ").append(rep.getDoctorName()).append("\n");
                context.append("   Diagnosis: ").append(safe(rep.getDiagnosis())).append("\n");
                context.append("   Prescription: ").append(safe(rep.getPrescription())).append("\n");
                context.append("   Advice: ").append(safe(rep.getAdvice())).append("\n");

                if (notEmpty(rep.getFollowUpDate())) {
                    context.append("   Follow-up Date: ").append(rep.getFollowUpDate()).append("\n");
                }

                // Scan-specific clinical fields for detailed AI answers
                if ("brain".equalsIgnoreCase(rep.getAnalysisType())) {
                    if (notEmpty(rep.getBrainTumorType()))
                        context.append("   Tumor Type: ").append(rep.getBrainTumorType()).append("\n");
                    if (notEmpty(rep.getBrainLocation()))
                        context.append("   Tumor Location: ").append(rep.getBrainLocation()).append("\n");
                    if (notEmpty(rep.getBrainWhoGrade()))
                        context.append("   WHO Grade: ").append(rep.getBrainWhoGrade()).append("\n");
                } else if ("spleen".equalsIgnoreCase(rep.getAnalysisType())) {
                    if (notEmpty(rep.getSpleenSize()))
                        context.append("   Spleen Size: ").append(rep.getSpleenSize()).append("\n");
                    if (notEmpty(rep.getIchFinding()))
                        context.append("   ICH Finding: ").append(rep.getIchFinding()).append("\n");
                } else if ("lungs".equalsIgnoreCase(rep.getAnalysisType())) {
                    if (notEmpty(rep.getLungFinding()))
                        context.append("   Lung Finding: ").append(rep.getLungFinding()).append("\n");
                    if (notEmpty(rep.getLungNoduleSize()))
                        context.append("   Nodule Size: ").append(rep.getLungNoduleSize()).append("\n");
                }

                context.append("\n");
            }
        }

        return context.toString();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private String safe(String val) {
        return (val != null && !val.isBlank()) ? val : "N/A";
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isBlank() && !s.equalsIgnoreCase("N/A");
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return "Routine";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private String typeLabel(String t) {
        if (t == null) return "Radiology";
        return switch (t.toLowerCase()) {
            case "brain"  -> "Brain MRI";
            case "spleen" -> "CT Spleen / ICH";
            case "lungs"  -> "Lung CT";
            default       -> t;
        };
    }
}