package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表 — 统一存储管理员、教师、学生三种角色
 * 角色通过 role 字段区分: 0=管理员, 1=教师, 2=学生
 * 密码使用 BCrypt 加密存储，不可逆
 */
@Data  // Lombok: 自动生成getter/setter/toString/equals/hashCode
@TableName("user")  // MyBatis-Plus: 指定数据库表名
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)  // 主键自增策略: 由数据库AUTO_INCREMENT控制
    private Integer id;

    private String username;   // 登录账号
    private String password;   // BCrypt加密密文(即使相同密码每次加密结果也不同)
    private String realName;   // 真实姓名，前端显示用
    private Integer role;      // 0=管理员(ADMIN), 1=教师(TEACHER), 2=学生(STUDENT)
    private String email;      // 邮箱(用于接收选课通知邮件)
    private String phone;      // 手机号(用于忘记密码时验证身份)
    private Integer disabled;  // 0=正常, 1=禁用(被禁用用户无法登录)
    private Date createTime;   // 账户创建时间
}
