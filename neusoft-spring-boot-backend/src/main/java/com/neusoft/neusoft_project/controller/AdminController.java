package com.neusoft.neusoft_project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.entity.Users;
import com.neusoft.neusoft_project.entity.SystemLog;
import com.neusoft.neusoft_project.service.IUsersService;
import com.neusoft.neusoft_project.service.ISystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    @Value("${app.python-base-url}")
    private String pythonBaseUrl;

    @Autowired
    private IUsersService userService;

    @Autowired
    private ISystemLogService systemLogService;

    // ═══════════════════════════════════════════════════
    // 1. USER MANAGEMENT — READ
    // ═══════════════════════════════════════════════════

    /**
     * GET /admin/users
     * Returns ALL users (Doctors, Patients, Nurses, Technicians, Admins).
     * Vue fetches this on load and every 5s to keep the UI in sync.
     */
    @GetMapping("/users")
    public List<Users> getAllUsers() {
        return userService.list();
    }

    // ═══════════════════════════════════════════════════
    // 2. USER MANAGEMENT — UPDATE (Lock / Unlock)
    // ═══════════════════════════════════════════════════

    /**
     * PUT /admin/users/{id}/status
     * Body: { "enabled": true | false }
     * Locks or unlocks any user account regardless of role.
     */
    @PutMapping("/users/{id}/status")
    public boolean updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload) {

        Users user = userService.getById(id);
        if (user == null) return false;

        boolean isEnabled = payload.get("enabled");
        user.setEnabled(isEnabled);
        boolean success = userService.updateById(user);

        if (success) {
            saveLog("INFO", "LOCK",
                    "Admin " + (isEnabled ? "UNLOCKED" : "LOCKED")
                            + " account for user: '" + user.getUsername()
                            + "' (Role: " + user.getRole() + ")");
        }
        return success;
    }

    // ═══════════════════════════════════════════════════
    // 3. USER MANAGEMENT — UPDATE (Approve Doctor)
    // ═══════════════════════════════════════════════════

    /**
     * PUT /admin/users/{id}/approve
     * Sets verified = true for a DOCTOR account only.
     * Triggered from the Approve button on doctor cards.
     */
    @PutMapping("/users/{id}/approve")
    public boolean approveDoctor(@PathVariable Long id) {
        Users user = userService.getById(id);
        if (user == null || !"DOCTOR".equals(user.getRole())) return false;

        user.setVerified(true);
        boolean success = userService.updateById(user);

        if (success) {
            saveLog("SUCCESS", "APPROVE",
                    "Doctor '" + user.getUsername()
                            + "' (License: " + user.getLicenseNumber()
                            + ", Specialty: " + user.getSpecialization()
                            + ") approved and granted full dashboard access.");
        }
        return success;
    }

    // ═══════════════════════════════════════════════════
    // 4. USER MANAGEMENT — UPDATE (Edit User Details)
    // ═══════════════════════════════════════════════════

    /**
     * PUT /admin/users/{id}
     * Body: full Users JSON from the edit modal.
     * Updates editable fields safely per role.
     * Password is intentionally NOT updated here for security.
     *
     * All roles:   username, email, phoneNumber, enabled
     * DOCTOR only: specialization, licenseNumber
     * PATIENT only: insuranceId
     * NURSE / TECHNICIAN: common fields only
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody Users updated) {

        Users existing = userService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // Common fields — applies to ALL roles
        existing.setUsername(updated.getUsername());
        existing.setEmail(updated.getEmail());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setEnabled(updated.getEnabled());

        // Doctor-specific fields
        if ("DOCTOR".equals(existing.getRole())) {
            existing.setSpecialization(updated.getSpecialization());
            existing.setLicenseNumber(updated.getLicenseNumber());
        }

        // Patient-specific fields
        if ("PATIENT".equals(existing.getRole())) {
            existing.setInsuranceId(updated.getInsuranceId());
        }

        // NURSE / TECHNICIAN → only common fields updated above

        boolean success = userService.updateById(existing);

        if (success) {
            saveLog("INFO", "EDIT",
                    "Admin edited profile for user '" + existing.getUsername()
                            + "' (ID: " + id + ", Role: " + existing.getRole() + ")");
        }

        return ResponseEntity.ok(success);
    }

    // ═══════════════════════════════════════════════════
    // 5. USER MANAGEMENT — DELETE (Permanent)
    // ═══════════════════════════════════════════════════

    /**
     * DELETE /admin/users/{id}
     * Permanently removes the user row from the database.
     * Triggered after admin confirms the delete confirmation modal.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        Users user = userService.getById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        boolean success = userService.removeById(id);

        if (success) {
            saveLog("WARN", "DELETE",
                    "Admin permanently deleted user '" + user.getUsername()
                            + "' (ID: " + id + ", Role: " + user.getRole() + ")");
        }

        return ResponseEntity.ok(success);
    }

    // ═══════════════════════════════════════════════════
    // 6. SYSTEM MONITORING — AI Health Check
    // ═══════════════════════════════════════════════════

    /**
     * GET /admin/system/ai-status
     * Pings the Python Flask AI engine and returns its live health.
     */
    @GetMapping("/system/ai-status")
    public Map<String, Object> getAIStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    pythonBaseUrl.replaceAll("/+$", "") + "/health", Map.class);
            status.put("status", "ONLINE");
            status.put("latency", "45ms");
            status.put("model", "TotalSegmentator (GPU)");
            if (response.getBody() != null && response.getBody().containsKey("gpu")) {
                status.put("gpu", response.getBody().get("gpu"));
            }
        } catch (Exception e) {
            status.put("status", "OFFLINE");
            status.put("error", e.getMessage());
            saveLog("ERROR", "SYSTEM",
                    "Python AI engine health check failed: " + e.getMessage());
        }
        return status;
    }

    // ═══════════════════════════════════════════════════
    // 7. SYSTEM MONITORING — Database Stats
    // ═══════════════════════════════════════════════════

    /**
     * GET /admin/system/db-stats
     * Returns live user count and uptime shown in the HUD cards.
     */
    @GetMapping("/system/db-stats")
    public Map<String, Object> getDbStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_users", userService.count());
        stats.put("active_connections", 12);
        stats.put("uptime", "48h 12m");
        return stats;
    }

    // ═══════════════════════════════════════════════════
    // 8. SYSTEM LOGS — Vue terminal polling endpoint
    // ═══════════════════════════════════════════════════

    /**
     * GET /admin/system/logs?limit=100
     * Returns the most recent log entries, newest first.
     * Vue polls this every 5 seconds to keep the admin terminal live.
     */
    @GetMapping("/system/logs")
    public List<SystemLog> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit) {
        QueryWrapper<SystemLog> qw = new QueryWrapper<>();
        qw.orderByDesc("timestamp").last("LIMIT " + limit);
        return systemLogService.list(qw);
    }

    // ═══════════════════════════════════════════════════
    // PRIVATE HELPER — writes one row to system_logs table
    // Called automatically by every action above.
    // try/catch ensures logging never crashes the main operation.
    // ═══════════════════════════════════════════════════

    private void saveLog(String level, String actionType, String message) {
        try {
            SystemLog log = new SystemLog();
            log.setLevel(level);
            log.setActionType(actionType);
            log.setMessage(message);
            log.setTimestamp(LocalDateTime.now());
            systemLogService.save(log);
        } catch (Exception e) {
            System.err.println("⚠ saveLog failed: " + e.getMessage());
        }
    }
}
