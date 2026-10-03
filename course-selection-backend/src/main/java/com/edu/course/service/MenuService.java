package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.entity.Menu;
import com.edu.course.mapper.MenuMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 菜单服务
 */
@Slf4j
@Service
public class MenuService {

    @Autowired
    private MenuMapper menuMapper;

    /**
     * 获取菜单树（带缓存）
     */
    @Cacheable(value = "menuTree")
    public List<Menu> getMenuTree() {
        log.info("🔴 [Redis] 缓存未命中 menuTree，从 MySQL 查询并写入 Redis");
        List<Menu> allMenus = menuMapper.selectList(null);
        return allMenus;
    }

    /**
     * 获取所有启用菜单
     */
    public List<Menu> getEnabledMenus() {
        LambdaQueryWrapper<Menu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Menu::getDisabled, 0).orderByAsc(Menu::getSort);
        return menuMapper.selectList(wrapper);
    }

    @CacheEvict(value = "menuTree", allEntries = true)
    public void addMenu(Menu menu) {
        menuMapper.insert(menu);
        log.info("🔴 [Redis] 清除缓存 menuTree（原因：新增菜单「{}」）", menu.getName());
    }

    @CacheEvict(value = "menuTree", allEntries = true)
    public void updateMenu(Menu menu) {
        menuMapper.updateById(menu);
        log.info("🔴 [Redis] 清除缓存 menuTree（原因：更新菜单「{}」）", menu.getName());
    }

    @CacheEvict(value = "menuTree", allEntries = true)
    public void toggleDisabled(Integer id, Integer disabled) {
        Menu menu = menuMapper.selectById(id);
        if (menu != null) {
            menu.setDisabled(disabled);
            menuMapper.updateById(menu);
            log.info("🔴 [Redis] 清除缓存 menuTree（原因：{}菜单「{}」）", disabled == 1 ? "禁用" : "启用", menu.getName());
        }
    }

    @CacheEvict(value = "menuTree", allEntries = true)
    public void deleteMenu(Integer id) {
        Menu menu = menuMapper.selectById(id);
        menuMapper.deleteById(id);
        if (menu != null) {
            log.info("🔴 [Redis] 清除缓存 menuTree（原因：删除菜单「{}」）", menu.getName());
        }
    }
}
