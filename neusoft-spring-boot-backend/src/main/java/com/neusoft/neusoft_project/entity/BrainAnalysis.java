package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("brain_analysis")
public class BrainAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;
    private String patientName;

    // This ID links to the Python backend folder
    private String caseId;

    private String status; // e.g., "COMPLETED", "PENDING"
    private LocalDateTime analysisDate;
}