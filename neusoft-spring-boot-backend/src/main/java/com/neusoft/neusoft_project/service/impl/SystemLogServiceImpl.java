package com.neusoft.neusoft_project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neusoft.neusoft_project.entity.SystemLog;
import com.neusoft.neusoft_project.mapper.SystemLogMapper;
import com.neusoft.neusoft_project.service.ISystemLogService;
import org.springframework.stereotype.Service;

@Service
public class SystemLogServiceImpl extends ServiceImpl<SystemLogMapper, SystemLog>
        implements ISystemLogService {
    // ServiceImpl + IService handle everything automatically
    // Nothing extra needed here
}