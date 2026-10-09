package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * User Entity - Updated with Email and Phone Number
 * </p>
 *
 * @author AI_Engineer
 * @since 2025-12-24
 */
@Getter
@Setter
@TableName("users")
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    // --- NEWLY ADDED FIELDS ---
    @TableField("email")
    private String email;

    @TableField("phone_number")
    private String phoneNumber;
    // --------------------------

    @TableField("role")
    private String role; // Values: 'ADMIN', 'DOCTOR', 'PATIENT', 'TECHNICIAN'

    @TableField("created_at")
    private LocalDateTime createdAt;

    // --- DOCTOR & PATIENT SPECIFIC FIELDS ---

    @TableField("specialization")
    private String specialization;

    @TableField("license_number")
    private String licenseNumber;

    @TableField("insurance_id")
    private String insuranceId;

    // --- ADMIN DASHBOARD FIELDS ---

    /**
     * Account Status
     * true  = Active / Unlocked (User can login)
     * false = Locked / Banned (Login denied)
     */
    @TableField("enabled")
    private Boolean enabled = true; // Default to true

    /**
     * Verification Status (Crucial for Doctors)
     * true  = Approved by Admin
     * false = Pending Approval (Limited access)
     */
    @TableField("verified")
    private Boolean verified = false; // Default to false
}