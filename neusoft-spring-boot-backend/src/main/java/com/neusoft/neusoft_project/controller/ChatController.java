package com.neusoft.neusoft_project.controller;

import com.neusoft.neusoft_project.service.PatientDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class ChatController {

    @Autowired
    private PatientDataService patientDataService;

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.patient-chat-model}")
    private String patientChatModel;

    // Per-session history — key: "patient_<id>" or "guest_<sessionId>"
    private final Map<String, List<String>> sessionHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_PROMPT_CHARS  = 12_000;

    // ── System prompts ────────────────────────────────────────────────────────
    private static final String SYS_PATIENT =
            "You are the NeuroPACS Medical Assistant for a hospital patient portal.\n" +
                    "You have access to the patient's real medical records below.\n\n" +
                    "YOUR RULES:\n" +
                    "1. Answer ONLY from the provided patient records. Never invent information.\n" +
                    "2. For appointment questions (status, date, doctor), read the APPOINTMENTS section.\n" +
                    "3. For diagnosis/report questions, read the MEDICAL REPORTS section.\n" +
                    "4. If the patient says they want to VIEW or SEE a report, respond with SHOW_REPORT:<report_id> " +
                    "   on a line by itself, then give a brief explanation.\n" +
                    "5. If information is not in the records, say clearly 'I don't have that information in your records.'\n" +
                    "6. Be warm, concise, and professional. Use simple language.\n" +
                    "7. Offer a helpful follow-up tip when relevant.\n";

    private static final String SYS_GUEST =
            "You are the NeuroPACS Hospital Support Bot.\n" +
                    "Answer ONLY general questions about registration, login, booking appointments, " +
                    "and general hospital information.\n" +
                    "Never discuss medical diagnoses or patient records.\n";

    // =========================================================================
    // POST /api/chat/ask
    // =========================================================================
    @PostMapping("/ask")
    public Map<String, Object> askAI(@RequestBody Map<String, Object> payload) {
        try {
            String userMessage = (String) payload.get("message");
            String patientId   = payload.get("patientId") != null
                    ? payload.get("patientId").toString().trim() : null;
            String sessionId   = payload.get("sessionId") != null
                    ? payload.get("sessionId").toString() : "default";

            if (userMessage == null || userMessage.isBlank()) {
                return Map.of("reply", "Please type a message.", "action", "", "actionId", "");
            }

            String systemPrompt, contextBlock, sessionKey;

            if (patientId != null && !patientId.isEmpty() && !patientId.equals("guest")) {
                String rawRecords = patientDataService.getPatientRecords(patientId);
                contextBlock = buildContextBlock(rawRecords);
                systemPrompt = SYS_PATIENT;
                sessionKey   = "patient_" + patientId;
            } else {
                contextBlock = "";
                systemPrompt = SYS_GUEST;
                sessionKey   = "guest_" + sessionId;
            }

            return callOllama(systemPrompt, contextBlock, userMessage, sessionKey);

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("reply", "Server error: " + e.getMessage(), "action", "", "actionId", "");
        }
    }

    private String buildContextBlock(String records) {
        if (records == null || records.isBlank()) return "";
        return "\n[PATIENT MEDICAL RECORDS — USE ONLY THESE AS SOURCE OF TRUTH]\n"
                + records.trim()
                + "\n[END OF PATIENT RECORDS]\n\n";
    }

    private Map<String, Object> callOllama(String systemPrompt, String contextBlock,
                                           String userMessage, String sessionKey) {
        try {
            List<String> history = sessionHistory.getOrDefault(sessionKey, Collections.emptyList());
            int fromIdx = Math.max(0, history.size() - MAX_HISTORY_TURNS);
            String historyText = history.isEmpty()
                    ? ""
                    : String.join("\n", history.subList(fromIdx, history.size())) + "\n";

            String fullPrompt = systemPrompt + "\n\n"
                    + contextBlock
                    + historyText
                    + "Patient: " + userMessage
                    + "\nAssistant:";

            String safePrompt = trimPrompt(fullPrompt, MAX_PROMPT_CHARS);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature",    0.2);
            options.put("top_p",          0.85);
            options.put("repeat_penalty", 1.2);
            options.put("num_predict",    500);

            Map<String, Object> body = new HashMap<>();
            body.put("model",   patientChatModel);
            body.put("prompt",  safePrompt);
            body.put("stream",  false);
            body.put("options", options);

            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = rt.postForEntity(ollamaBaseUrl.replaceAll("/+$", "") + "/api/generate",
                    new HttpEntity<>(body, headers), Map.class);

            if (response.getBody() == null || !response.getBody().containsKey("response")) {
                return Map.of("reply", "AI did not respond. Please try again.", "action", "", "actionId", "");
            }

            String rawReply = (String) response.getBody().get("response");
            String cleaned  = rawReply.replaceAll("(?s)<think>.*?</think>", "").trim();
            if (cleaned.isEmpty()) cleaned = "I'm processing your records...";

            // Detect SHOW_REPORT:<id> command from the model
            String action   = "";
            String actionId = "";

            if (cleaned.contains("SHOW_REPORT:")) {
                int idx       = cleaned.indexOf("SHOW_REPORT:");
                String after  = cleaned.substring(idx + 12).trim();
                String repId  = after.replaceAll("[^0-9].*", "").trim();
                if (!repId.isEmpty()) {
                    action   = "SHOW_REPORT";
                    actionId = repId;
                }
                cleaned = cleaned.substring(0, idx).trim();
                if (cleaned.isEmpty()) cleaned = "Opening your report now...";
            }

            // Save to history
            List<String> hist = sessionHistory.computeIfAbsent(sessionKey, k -> new ArrayList<>());
            hist.add("Patient: " + userMessage);
            hist.add("Assistant: " + cleaned);
            while (hist.size() > MAX_HISTORY_TURNS * 2) hist.remove(0);

            Map<String, Object> result = new HashMap<>();
            result.put("reply",    cleaned);
            result.put("action",   action);
            result.put("actionId", actionId);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("reply", "AI Service Unavailable. Is Ollama running?", "action", "", "actionId", "");
        }
    }

    private String trimPrompt(String prompt, int maxChars) {
        if (prompt.length() <= maxChars) return prompt;
        return prompt.substring(prompt.length() - maxChars);
    }
}
