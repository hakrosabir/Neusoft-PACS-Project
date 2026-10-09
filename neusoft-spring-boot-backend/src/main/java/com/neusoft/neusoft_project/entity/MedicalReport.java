package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("medical_report")
public class MedicalReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    // ── Core appointment / person refs ──────────────────────────────
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String patientName;
    private String doctorName;
    private LocalDateTime reportDate;

    // ── Clinical content ─────────────────────────────────────────────
    private String diagnosis;
    private String prescription;
    private String advice;

    // ── Report meta ──────────────────────────────────────────────────
    private String analysisType;   // "brain" | "spleen" | "lungs"
    private String severity;       // "routine" | "urgent" | "critical"
    private String followUpDate;   // stored as plain string "YYYY-MM-DD"

    // ── Scan images  (store as LONGTEXT in DB) ────────────────────────
    private String axialImage;
    private String coronalImage;
    private String sagittalImage;
    private String mesh3dImage;    // 3D segmentation / lobe map

    // ── Brain MRI fields ─────────────────────────────────────────────
    private String brainTumorType;
    private String brainWhoGrade;
    private String brainLocation;
    private String brainVolume;
    private String brainEnhancing;

    // ── CT Spleen / ICH fields ───────────────────────────────────────
    private String spleenSize;
    private String spleenLength;
    private String ichFinding;
    private String spleenParenchyma;

    // ── Lung CT fields ───────────────────────────────────────────────
    private String lungFinding;
    private String lungLobes;
    private String lungNoduleSize;
    private String lungPleural;
    private String lungLobeSummary;
}