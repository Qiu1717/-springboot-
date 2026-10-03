package com.edu.course.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.entity.Menu;
import com.edu.course.entity.RoleMenu;
import com.edu.course.entity.User;
import com.edu.course.mapper.MenuMapper;
import com.edu.course.mapper.RoleMenuMapper;
import com.edu.course.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义UserDetailsService - 从数据库加载用户信息及role_menu权限
 */
@Service  // 实现Spring Security的UserDetailsService接口，替代默认的内存用户
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;  // 用户表Mapper

    @Autowired
    private RoleMenuMapper roleMenuMapper;  // 角色-菜单关联表Mapper(RBAC权限模型)

    @Autowired
    private MenuMapper menuMapper;  // 菜单表Mapper(存储权限编码如admin:notice:manage)

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // MyBatis-Plus 条件构造器: 构建 WHERE username = ?
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);  // Lambda方式指定字段，避免字符串硬编码
        User user = userMapper.selectOne(wrapper);  // 查询单条用户记录

        if (user == null || user.getDisabled() == 1) {  // 用户不存在或被禁用(t_user.disabled=1)
            throw new UsernameNotFoundException("用户不存在或已被禁用");
        }

        // ===== 第一步: 加载Spring Security基础角色权限(ROLE_前缀) =====
        String roleCode;
        switch (user.getRole()) {  // t_user.role: 0=管理员, 1=教师, 2=学生
            case 0: roleCode = "ROLE_ADMIN"; break;    // Spring Security要求角色以ROLE_开头
            case 1: roleCode = "ROLE_TEACHER"; break;
            default: roleCode = "ROLE_STUDENT";
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(roleCode));  // 添加基础角色(用于hasRole检查)

        // ===== 第二步: 从role_menu表加载细粒度菜单权限(如admin:notice:manage) =====
        // role=0(ADMIN)对应role_id=1, role=1(TEACHER)→role_id=2, role=2(STUDENT)→role_id=3
        Integer roleId = user.getRole() + 1;
        LambdaQueryWrapper<RoleMenu> rmWrapper = new LambdaQueryWrapper<>();
        rmWrapper.eq(RoleMenu::getRoleId, roleId);  // WHERE role_id = ?
        List<RoleMenu> roleMenus = roleMenuMapper.selectList(rmWrapper);  // 查出该角色关联的所有菜单
        if (roleMenus != null && !roleMenus.isEmpty()) {
            // 提取menu_id列表
            List<Integer> menuIds = roleMenus.stream().map(RoleMenu::getMenuId).collect(Collectors.toList());
            List<Menu> menus = menuMapper.selectBatchIds(menuIds);  // 批量查询菜单详情(含permission字段)
            for (Menu m : menus) {
                // 只加载已启用且权限编码非空的菜单
                if (m.getPermission() != null && !m.getPermission().isEmpty() && m.getDisabled() == 0) {
                    authorities.add(new SimpleGrantedAuthority(m.getPermission()));  // 添加细粒度权限(用于hasAuthority检查)
                }
            }
        }

        // 返回Spring Security标准的UserDetails对象(用户名、BCrypt加密密码、权限列表)
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),  // 数据库中已是BCrypt密文
                authorities  // 包含ROLE_xxx + 各菜单permission编码
        );
    }
}
