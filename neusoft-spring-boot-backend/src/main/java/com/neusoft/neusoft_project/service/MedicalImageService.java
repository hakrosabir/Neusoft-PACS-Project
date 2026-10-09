package com.neusoft.neusoft_project.service;

import com.neusoft.neusoft_project.entity.MedicalReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import java.util.*;

@Service
public class MedicalImageService {

    @Autowired
    private IMedicalReportService reportService;

    // URL of your Python Flask App
    @Value("${app.python-base-url}")
    private String pythonBaseUrl;

    public Map<String, String> processMedicalImage(MultipartFile file, String patientId, String doctorId) {
        Map<String, String> result = new HashMap<>();

        try {
            // 1. Prepare Header & Resource
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Wraps the file bytes to send to Python
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename(); // Python needs the name
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 2. Send to Python
            String segmentationUrl = pythonBaseUrl.replaceAll("/+$", "") + "/api/totalseg_start";
            System.out.println("Forwarding file to Python: " + segmentationUrl);
            ResponseEntity<Map> pythonResponse = restTemplate.postForEntity(segmentationUrl, requestEntity, Map.class);

            if (pythonResponse.getStatusCode() == HttpStatus.OK && pythonResponse.getBody() != null) {
                String caseId = (String) pythonResponse.getBody().get("case_id");

                // 3. Create Preliminary Report
                MedicalReport report = new MedicalReport();
                report.setPatientId(parseId(patientId));
                report.setDoctorId(parseId(doctorId));
                report.setPatientName("Patient " + patientId);
                report.setDoctorName("Dr. " + doctorId);
                report.setDiagnosis("AI Segmentation In Progress (Case: " + caseId + ")");
                report.setAdvice("Waiting for TotalSegmentator results...");
                report.setReportDate(java.time.LocalDateTime.now());

                // Save CaseID temporarily in image field
                report.setAxialImage("CASE_ID:" + caseId);

                reportService.save(report);

                result.put("status", "success");
                result.put("caseId", caseId);
                result.put("reportId", report.getId().toString());
            } else {
                result.put("status", "error");
                result.put("message", "Python returned error.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "error");
            result.put("message", "Backend Bridge Error: " + e.getMessage());
        }
        return result;
    }

    private Long parseId(String id) {
        try { return Long.parseLong(id); } catch (Exception e) { return 0L; }
    }
}
