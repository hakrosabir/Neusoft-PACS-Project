package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;

@TableName("appointments")
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("patient_id")
    private Long patientId;

    @TableField("patient_name")
    private String patientName;

    @TableField("doctor_id")
    private Long doctorId;

    @TableField("doctor_name")
    private String doctorName;

    @TableField("department")
    private String department;

    @TableField("appointment_date")
    private LocalDate appointmentDate;

    @TableField("time_slot")
    private String timeSlot;

    @TableField("status")
    private String status;

    // --- FIELDS FOR NURSE/TECH WORKFLOW ---

    @TableField("scan_file")
    private String scanFile;

    @TableField("nurse_notes")
    private String nurseNotes;

    @TableField("priority")
    private String priority;

    // --- NEW FIELDS FOR PATIENT INFO (SNAPSHOT) ---

    /**
     * Unique Patient ID for DICOM matching.
     * Example: "PAT-2024-001"
     */
    @TableField("dicom_patient_id")
    private String dicomPatientId;

    /**
     * Patient Gender.
     * Values: 'MALE', 'FEMALE', 'OTHER'
     */
    @TableField("patient_gender")
    private String patientGender;

    /**
     * Patient Date of Birth.
     */
    @TableField("patient_birth_date")
    private LocalDate patientBirthDate;

    /**
     * Extra metadata in JSON format.
     */
    @TableField("meta_data")
    private String metaData;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getScanFile() { return scanFile; }
    public void setScanFile(String scanFile) { this.scanFile = scanFile; }

    public String getNurseNotes() { return nurseNotes; }
    public void setNurseNotes(String nurseNotes) { this.nurseNotes = nurseNotes; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    // --- New Getters and Setters for Patient Info ---

    public String getDicomPatientId() { return dicomPatientId; }
    public void setDicomPatientId(String dicomPatientId) { this.dicomPatientId = dicomPatientId; }

    public String getPatientGender() { return patientGender; }
    public void setPatientGender(String patientGender) { this.patientGender = patientGender; }

    public LocalDate getPatientBirthDate() { return patientBirthDate; }
    public void setPatientBirthDate(LocalDate patientBirthDate) { this.patientBirthDate = patientBirthDate; }

    public String getMetaData() { return metaData; }
    public void setMetaData(String metaData) { this.metaData = metaData; }

    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", patientId=" + patientId +
                ", doctorId=" + doctorId +
                ", status='" + status + '\'' +
                ", scanFile='" + scanFile + '\'' +
                ", dicomPatientId='" + dicomPatientId + '\'' +
                ", patientGender='" + patientGender + '\'' +
                '}';
    }
}