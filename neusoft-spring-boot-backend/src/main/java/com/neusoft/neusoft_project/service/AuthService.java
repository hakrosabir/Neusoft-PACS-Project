package com.neusoft.neusoft_project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.common.Result;
import com.neusoft.neusoft_project.entity.SystemLog;
import com.neusoft.neusoft_project.entity.Users;
import com.neusoft.neusoft_project.mapper.UsersMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // ← ONLY NEW LINE ADDED: inject the log service
    @Autowired
    private ISystemLogService systemLogService;

    /**
     * LOGIN LOGIC — unchanged except logAudit now saves to DB
     */
    public Result login(String username, String password) {
        // 1. Find user by username
        QueryWrapper<Users> query = new QueryWrapper<>();
        query.eq("username", username);
        Users user = usersMapper.selectOne(query);

        // 2. Validate User Existence & Password
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            logAudit("WARN", username, "LOGIN_FAILED",
                    "Failed login attempt — invalid username or password.");
            return Result.error("Invalid Username or Password");
        }

        // 3. Check Account Lock Status (Admin Ban)
        if (Boolean.FALSE.equals(user.getEnabled())) {
            logAudit("WARN", username, "LOGIN_DENIED",
                    "Login blocked — account locked by Admin. Role: " + user.getRole());
            return Result.error("Access Denied: Your account has been locked.");
        }

        // 4. Check Verification (Doctors need approval)
        if ("DOCTOR".equalsIgnoreCase(user.getRole()) && !Boolean.TRUE.equals(user.getVerified())) {
            logAudit("WARN", username, "LOGIN_DENIED",
                    "Login blocked — Doctor account pending Admin approval.");
            return Result.error("Access Pending: Your account is awaiting Admin approval.");
        }

        // 5. Login Success — generate token
        String token = "JWT-" + user.getId() + "-" + System.currentTimeMillis();

        // 6. Return token + user info
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);

        logAudit("INFO", username, "LOGIN_SUCCESS",
                "User '" + username + "' logged in successfully. Role: " + user.getRole());
        return Result.success(data);
    }

    /**
     * REGISTRATION LOGIC — unchanged except logAudit now saves to DB
     */
    public Result register(Users user) {
        // 1. Check if username already exists
        QueryWrapper<Users> query = new QueryWrapper<>();
        query.eq("username", user.getUsername());
        if (usersMapper.selectCount(query) > 0) {
            logAudit("WARN", user.getUsername(), "REGISTER_FAILED",
                    "Registration attempt failed — username '" + user.getUsername() + "' already exists.");
            return Result.error("Username already exists");
        }

        // 2. Encrypt Password
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 3. Set System Fields
        user.setCreatedAt(LocalDateTime.now());

        // 4. Set Permissions based on Role
        if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
            user.setVerified(false);
            user.setEnabled(true);
        } else {
            user.setVerified(true);
            user.setEnabled(true);
        }

        // 5. Save to Database
        usersMapper.insert(user);

        // 6. Log with full details
        String roleDetail = "DOCTOR".equalsIgnoreCase(user.getRole())
                ? "Doctor registered — PENDING admin approval. License: " + user.getLicenseNumber()
                : user.getRole() + " registered — auto-approved and active.";

        logAudit("INFO", user.getUsername(), "REGISTER",
                "New user '" + user.getUsername() + "' registered. " + roleDetail);

        return Result.success("User registered successfully");
    }

    /**
     * logAudit — saves every event permanently to system_logs table.
     *
     * @param level      "INFO" | "WARN" | "ERROR" | "SUCCESS"
     * @param username   who triggered the event
     * @param actionType "LOGIN_SUCCESS" | "LOGIN_FAILED" | "REGISTER" | "LOGIN_DENIED" | etc.
     * @param message    full human-readable description shown in the admin terminal
     */
    private void logAudit(String level, String username, String actionType, String message) {
        try {
            SystemLog log = new SystemLog();
            log.setLevel(level);
            log.setActionType(actionType);
            // Prefix message with username so admin can see WHO did WHAT
            log.setMessage("[" + username + "] " + message);
            log.setTimestamp(LocalDateTime.now());
            systemLogService.save(log);
        } catch (Exception e) {
            // Fallback: if DB write fails, at least print to console so nothing crashes
            System.err.println("AUDIT LOG FAILED: " + message + " | Error: " + e.getMessage());
        }
    }
}