package com.neusoft.neusoft_project.mapper;

import com.neusoft.neusoft_project.entity.PatientProfiles;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 李如一 2428042
 * @since 2025-12-18
 */

@Mapper
@Repository

public interface PatientProfilesMapper extends BaseMapper<PatientProfiles> {

}
