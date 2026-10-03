package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 选课记录表
 */
@Data
@TableName("selection")
public class Selection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学生ID */
    private Integer studentId;

    /** 课程ID */
    private Integer courseId;

    /** 成绩 */
    private Float score;

    /** 选课时间 */
    private Date selectTime;
}
