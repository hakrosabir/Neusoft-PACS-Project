package com.neusoft.neusoft_project.mapper;

import com.neusoft.neusoft_project.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
    // MyBatis-Plus provides basic CRUD automatically (insert, selectById, updateById, deleteById)
    // We don't need to write custom SQL for basic tasks.
}