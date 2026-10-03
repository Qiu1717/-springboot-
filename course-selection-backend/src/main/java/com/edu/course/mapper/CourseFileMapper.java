package com.edu.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.course.entity.CourseFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程资料Mapper
 */
@Mapper
public interface CourseFileMapper extends BaseMapper<CourseFile> {
}
