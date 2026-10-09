package com.neusoft.neusoft_project.service.impl;

import com.neusoft.neusoft_project.entity.Appointment;
import com.neusoft.neusoft_project.mapper.AppointmentMapper;
import com.neusoft.neusoft_project.service.IAppointmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {
    // Core logic is handled by ServiceImpl
}