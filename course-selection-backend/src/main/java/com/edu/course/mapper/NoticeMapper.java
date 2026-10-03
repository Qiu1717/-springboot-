package com.edu.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.course.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告Mapper
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
