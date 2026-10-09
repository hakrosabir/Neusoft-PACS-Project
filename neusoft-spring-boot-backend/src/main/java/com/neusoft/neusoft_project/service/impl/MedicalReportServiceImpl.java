package com.neusoft.neusoft_project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neusoft_project.entity.MedicalReport;
import com.neusoft.neusoft_project.mapper.MedicalReportMapper;
import com.neusoft.neusoft_project.service.IMedicalReportService;
import org.springframework.stereotype.Service;

@Service
public class MedicalReportServiceImpl extends ServiceImpl<MedicalReportMapper, MedicalReport> implements IMedicalReportService {
}