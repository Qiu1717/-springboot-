package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 课程资料（文件）表
 */
@Data
@TableName("course_file")
public class CourseFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件扩展名 */
    private String fileExtension;

    /** 服务器存储路径 */
    private String filePath;

    /** 上传者ID */
    private Integer uploaderId;

    /** 上传者姓名 */
    private String uploaderName;

    /** 上传者角色：0管理员 1教师 */
    private Integer uploaderRole;

    /** 关联的课程ID */
    private Integer courseId;

    /** 上传时间 */
    private Date uploadTime;
}
