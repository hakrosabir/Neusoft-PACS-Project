package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * Patient Profiles Entity - Stores detailed patient demographic data.
 * </p>
 *
 * @author 李如一 2428042
 * @since 2025-12-18
 */
@TableName("patient_profiles")
public class PatientProfiles implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary Key, links to the 'users' table ID.
     */
    @TableId("user_id")
    private Long userId;

    /**
     * Official Date of Birth.
     */
    @TableField("date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Insurance Policy Number.
     */
    @TableField("insurance_id")
    private String insuranceId;

    // --- NEW FIELDS ADDED FOR APPOINTMENT SYNC ---

    /**
     * Unique Patient ID used in DICOM files (e.g., "PAT-10023").
     */
    @TableField("dicom_patient_id")
    private String dicomPatientId;

    /**
     * Full Name of the patient (can be de-identified if needed).
     */
    @TableField("full_name")
    private String fullName;

    /**
     * Gender/Sex (MALE, FEMALE, OTHER).
     */
    @TableField("gender")
    private String gender;

    /**
     * JSON string to store custom or extra fields (e.g., allergies, history).
     */
    @TableField("meta_data")
    private String metaData;

    // --- Getters and Setters ---

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getInsuranceId() {
        return insuranceId;
    }

    public void setInsuranceId(String insuranceId) {
        this.insuranceId = insuranceId;
    }

    // --- New Getters and Setters ---

    public String getDicomPatientId() {
        return dicomPatientId;
    }

    public void setDicomPatientId(String dicomPatientId) {
        this.dicomPatientId = dicomPatientId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    @Override
    public String toString() {
        return "PatientProfiles{" +
                "userId=" + userId +
                ", dateOfBirth=" + dateOfBirth +
                ", insuranceId='" + insuranceId + '\'' +
                ", dicomPatientId='" + dicomPatientId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", gender='" + gender + '\'' +
                ", metaData='" + metaData + '\'' +
                '}';
    }
}