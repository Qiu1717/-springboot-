package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色菜单关联表
 */
@Data
@TableName("role_menu")
public class RoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Integer roleId;

    /** 菜单ID */
    private Integer menuId;
}
