package com.neusoft.neusoft_project.controller; // UPDATED PACKAGE

import com.neusoft.neusoft_project.service.DoctorRagService; // ADDED IMPORT
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "http://localhost:5173")
public class DoctorRagController {

    private final DoctorRagService ragService;

    public DoctorRagController(DoctorRagService ragService) {
        this.ragService = ragService;
    }

    // New Endpoint: Upload PDF
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDoc(@RequestParam("file") MultipartFile file) {
        try {
            ragService.ingestPDF(new InputStreamResource(file.getInputStream()));
            return ResponseEntity.ok(Map.of("reply", "PDF Analyzed. You can now ask questions about it."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("reply", "Error processing PDF."));
        }
    }

    // New Endpoint: Chat with PDF
    @PostMapping("/ask")
    public Map<String, String> askDoc(@RequestBody Map<String, String> payload) {
        String answer = ragService.askDocument(payload.get("message"));
        return Map.of("reply", answer);
    }
}