package com.neusoft.neusoft_project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_logs")          // ← matches your SQL table name exactly
public class SystemLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String level;           // "INFO" | "WARN" | "ERROR" | "SUCCESS"

    @TableField("action_type")      // ← maps camelCase to snake_case column
    private String actionType;      // "LOGIN" | "LOGOUT" | "REGISTER" | "APPROVE" | "LOCK"

    private String message;         // Human-readable description

    private LocalDateTime timestamp; // Auto-filled by DB, but we also set it in Java
}