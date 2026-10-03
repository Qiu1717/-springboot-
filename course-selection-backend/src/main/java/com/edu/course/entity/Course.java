package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 课程表
 */
@Data
@TableName("course")
public class Course implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;       // 课程名称
    private Float credit;      // 学分(如2.0, 3.5)
    private Integer teacherId; // 执教教师ID(关联user表)
    private Integer capacity;  // 总容量(最多选课人数)
    private Integer selected;  // 已选人数(每次选课+1，退课-1，不能超过capacity)
    private String term;       // 学期 格式: "2025-2026-1"(2025-2026学年第一学期)
    private String schedule;   // 上课时间地点 如 "周一 1-2节 机房A101"
    private Integer status;    // 状态: 1=启用(学生可见), 0=停用(不可选)

    @TableField(exist = false)  // 标记为数据库不存在的字段，仅用于前端展示
    private String teacherName; // 教师姓名(CourseService.fillTeacherName()填充)
}
