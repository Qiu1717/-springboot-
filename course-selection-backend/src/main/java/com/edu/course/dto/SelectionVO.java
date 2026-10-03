package com.edu.course.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 选课记录VO — 用于前端展示的视图对象(多表联查结果)
 * 与 Selection 实体不同: 多出了 courseName/studentName/term/credit 展示字段
 * 数据来源: SelectionMapper 中的连表查询SQL
 */
@Data
public class SelectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Integer studentId;
    private String studentName;   // 学生姓名(从user表JOIN)
    private Integer courseId;
    private String courseName;    // 课程名(从course表JOIN)
    private Float score;          // 成绩(null=未出分)
    private Date selectTime;      // 选课时间
    private String term;          // 学期(从course表)
    private Float credit;         // 学分(从course表，用于统计已获学分)
}
