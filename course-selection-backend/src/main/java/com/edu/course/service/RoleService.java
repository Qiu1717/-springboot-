package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.entity.Menu;
import com.edu.course.entity.Role;
import com.edu.course.entity.RoleMenu;
import com.edu.course.mapper.MenuMapper;
import com.edu.course.mapper.RoleMapper;
import com.edu.course.mapper.RoleMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限服务 — RBAC(Role-Based Access Control)核心
 * 
 * 数据模型: role(角色) → role_menu(关联表) → menu(菜单/权限)
 * 每个角色可分配多个菜单权限，每个菜单有permission编码如 admin:notice:manage
 * 前端显示哪些菜单 + 后端@PreAuthorize根据permission控制接口访问
 */
@Service
public class RoleService {

    @Autowired private RoleMapper roleMapper;
    @Autowired private RoleMenuMapper roleMenuMapper;
    @Autowired private MenuMapper menuMapper;

    /** 删除角色 — 同时删除role_menu表中的关联记录(避免孤儿数据) */
    @Transactional
    public void deleteRole(Integer roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);  // 先删关联(WHERE role_id = ?)
        roleMapper.deleteById(roleId);   // 再删角色
    }

    /** 保存角色菜单关联 — 全量替换策略 */
    @Transactional
    public void saveRoleMenus(Integer roleId, List<Integer> menuIds) {
        // ① 先删除该角色所有旧关联
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        roleMenuMapper.delete(wrapper);  // DELETE FROM role_menu WHERE role_id = ?
        // ② 再逐条插入新关联(前端el-tree传来选中的menuIds)
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Integer menuId : menuIds) {
                RoleMenu rm = new RoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                roleMenuMapper.insert(rm);
            }
        }
    }

    /**
     * 根据用户角色查其有权访问的菜单 — 前端Layout.vue动态渲染侧边栏用
     * 
     * 过滤规则:
     *   ① role→role_menu→menu 三级关联
     *   ② 只返回启用(disabled=0)的菜单
     *   ③ 排除parent_id=0的容器菜单(如"管理员菜单"这类分组标题)
     *   ④ 按sort排序
     */
    public List<Menu> getMenusByUserRole(Integer userRole) {
        Integer roleId = userRole + 1;  // user.role(0/1/2) → role_menu.role_id(1/2/3)
        List<Integer> menuIds = getRoleMenuIds(roleId);  // 查出该角色拥有的menu_id列表
        if (menuIds.isEmpty()) return java.util.Collections.emptyList();  // 无菜单权限
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Menu::getId, menuIds)   // WHERE id IN (1,2,3,...)
                .eq(Menu::getDisabled, 0)  // AND disabled = 0(启用)
                .ne(Menu::getParentId, 0)  // AND parent_id != 0(排除容器菜单)
                .orderByAsc(Menu::getSort); // ORDER BY sort ASC
        return menuMapper.selectList(wrapper);
    }

    /** 获取所有角色 */
    public List<Role> getAllRoles() {
        return roleMapper.selectList(null);
    }

    /** 新增角色 */
    public void addRole(Role role) {
        roleMapper.insert(role);
    }

    /** 更新角色 */
    public void updateRole(Role role) {
        roleMapper.updateById(role);
    }

    /** 获取角色拥有的菜单ID列表 */
    public List<Integer> getRoleMenuIds(Integer roleId) {
        LambdaQueryWrapper<RoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenu::getRoleId, roleId);
        return roleMenuMapper.selectList(wrapper).stream()
                .map(RoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    /** 获取所有启用菜单（供角色分配用） */
    public List<Menu> getAllEnabledMenus() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getDisabled, 0).orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }
}
