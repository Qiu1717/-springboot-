package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内消息表
 */
@Data
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer receiverId;  // 接收者用户ID（关联user表）

    private String title;        // 消息标题（如"密码安全提醒"） 

    private String content;      // 消息正文

    private Integer isRead;      // 0=未读, 1=已读

    private Date createTime;     // 消息时间
}
