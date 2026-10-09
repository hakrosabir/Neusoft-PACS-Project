package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author 李如一 2428042
 * @since 2025-12-18
 */
@TableName("doctor_profiles")
public class DoctorProfiles implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("user_id")
    private Long userId;

    @TableField("specialization")
    private String specialization;

    @TableField("license_number")
    private String licenseNumber;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    @Override
    public String toString() {
        return "DoctorProfiles{" +
            "userId = " + userId +
            ", specialization = " + specialization +
            ", licenseNumber = " + licenseNumber +
        "}";
    }
}
