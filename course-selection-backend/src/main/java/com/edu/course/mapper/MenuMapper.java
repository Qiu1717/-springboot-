package com.edu.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.course.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}
