package com.neusoft.neusoft_project.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neusoft.neusoft_project.entity.BrainAnalysis;
import com.neusoft.neusoft_project.mapper.BrainAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/brain")
@CrossOrigin(origins = "http://localhost:5173")
public class BrainAnalysisController {

    @Autowired
    private BrainAnalysisMapper brainMapper;

    // 1. Link a Python Case ID to a Patient in SQL
    @PostMapping("/link")
    public boolean linkAnalysis(@RequestBody BrainAnalysis analysis) {
        if (analysis.getAnalysisDate() == null) {
            analysis.setAnalysisDate(LocalDateTime.now());
        }
        analysis.setStatus("COMPLETED");
        return brainMapper.insert(analysis) > 0;
    }

    // 2. Get all 3D analyses for a patient
    @GetMapping("/patient/{patientId}")
    public List<BrainAnalysis> getByPatient(@PathVariable Long patientId) {
        QueryWrapper<BrainAnalysis> query = new QueryWrapper<>();
        query.eq("patient_id", patientId).orderByDesc("analysis_date");
        return brainMapper.selectList(query);
    }

    // 3. Get all available cases (For the dropdown in Brain Studio)
    @GetMapping("/all")
    public List<BrainAnalysis> getAllCases() {
        QueryWrapper<BrainAnalysis> query = new QueryWrapper<>();
        query.orderByDesc("analysis_date");
        return brainMapper.selectList(query);
    }
}