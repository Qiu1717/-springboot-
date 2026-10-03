package com.edu.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单表（用于RBAC权限控制）
 */
@Data
@TableName("menu")
public class Menu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 菜单名称 */
    private String name;

    /** 前端路由 */
    private String path;

    /** 权限标识，如 admin:menu:add */
    private String permission;

    /** 父菜单ID */
    private Integer parentId;

    /** 排序 */
    private Integer sort;

    /** 0启用，1禁用 */
    private Integer disabled;
}
