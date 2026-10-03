package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.*;
import com.edu.course.service.*;
import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器 — 管理员和教师共用(通过role_menu分配权限)
 * 
 * 权限模型:
 *   @PreAuthorize 在类级别: 需要有6个管理权限码中的任意一个
 *   CustomUserDetailsService 从 role_menu 表加载权限 → 如果给TEACHER(role=1→role_id=2)
 *   分配了公告管理菜单，教师就拥有 admin:notice:manage 权限，可以访问此Controller
 *   各方法内部还有二次校验: if(user.getRole()==1) 只允许操作自己的数据
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyAuthority('admin:menu:manage','admin:notice:manage','admin:teacher:manage','admin:course:manage','admin:student:manage','admin:role:manage')")
public class AdminController {

    @Autowired
    private MenuService menuService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private JwtUtil jwtUtil;

    // ==================== 菜单管理 ====================

    @GetMapping("/menus")
    public Result<List<Menu>> getMenus() {
        return Result.success(menuService.getMenuTree());
    }

    @PostMapping("/menu/add")
    public Result<?> addMenu(@RequestBody Menu menu) {
        menuService.addMenu(menu);
        return Result.success();
    }

    @PutMapping("/menu/update")
    public Result<?> updateMenu(@RequestBody Menu menu) {
        menuService.updateMenu(menu);
        return Result.success();
    }

    @PutMapping("/menu/disable/{id}")
    public Result<?> disableMenu(@PathVariable Integer id) {
        menuService.toggleDisabled(id, 1);
        return Result.success();
    }

    @PutMapping("/menu/enable/{id}")
    public Result<?> enableMenu(@PathVariable Integer id) {
        menuService.toggleDisabled(id, 0);
        return Result.success();
    }

    @DeleteMapping("/menu/delete/{id}")
    public Result<?> deleteMenu(@PathVariable Integer id) {
        menuService.deleteMenu(id);
        return Result.success();
    }

    // ==================== 公告管理 ====================

    @GetMapping("/notices")
    public Result<List<Notice>> getNotices() {
        return Result.success(noticeService.getAllNotices());
    }

    @GetMapping("/notice/{id}")
    public Result<Notice> getNoticeById(@PathVariable Integer id) {
        return Result.success(noticeService.getById(id));
    }

    @PostMapping("/notice/add")
    public Result<?> addNotice(@RequestBody Notice notice, @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUsername(username);
        if (user != null) {
            // 管理员落款"教务处"，教师落款真实姓名
            notice.setAuthor(user.getRole() == 0 ? "教务处" : user.getRealName());
        }
        noticeService.addNotice(notice);
        return Result.success();
    }

    @PutMapping("/notice/update")
    public Result<?> updateNotice(@RequestBody Notice notice,
                                   @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        // 教师只能编辑自己发布的公告
        if (user != null && user.getRole() == 1) {
            Notice existing = noticeService.getById(notice.getId());
            if (existing != null && !user.getRealName().equals(existing.getAuthor())) {
                throw new com.edu.course.common.BusinessException("只能编辑自己发布的公告");
            }
        }
        noticeService.updateNotice(notice);
        return Result.success();
    }

    @DeleteMapping("/notice/delete/{id}")
    public Result<?> deleteNotice(@PathVariable Integer id,
                                   @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        if (user != null && user.getRole() == 1) {
            Notice existing = noticeService.getById(id);
            if (existing != null && !user.getRealName().equals(existing.getAuthor())) {
                throw new com.edu.course.common.BusinessException("只能删除自己发布的公告");
            }
        }
        noticeService.deleteNotice(id);
        return Result.success();
    }

    // ==================== 教师管理 ====================

    @GetMapping("/teachers")
    public Result<List<User>> getTeachers() {
        return Result.success(userService.getTeacherList());
    }

    @PostMapping("/teacher/add")
    public Result<?> addTeacher(@RequestBody User teacher) {
        userService.addTeacher(teacher);
        return Result.success();
    }

    @PutMapping("/teacher/disable/{id}")
    public Result<?> disableTeacher(@PathVariable Integer id) {
        userService.toggleDisabled(id, 1);
        return Result.success();
    }

    @PutMapping("/teacher/enable/{id}")
    public Result<?> enableTeacher(@PathVariable Integer id) {
        userService.toggleDisabled(id, 0);
        return Result.success();
    }

    // ==================== 课程管理 ====================

    @GetMapping("/courses")
    public Result<List<Course>> getCourses() {
        return Result.success(courseService.getAllCourses());
    }

    @PostMapping("/course/add")
    public Result<?> addCourse(@RequestBody Course course) {
        courseService.addCourse(course);
        return Result.success();
    }

    @PutMapping("/course/update")
    public Result<?> updateCourse(@RequestBody Course course,
                                   @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        if (user != null && user.getRole() == 1) {
            Course existing = courseService.getById(course.getId());
            if (existing != null && !existing.getTeacherId().equals(user.getId())) {
                throw new com.edu.course.common.BusinessException("只能编辑自己教授的课程");
            }
        }
        courseService.updateCourse(course);
        return Result.success();
    }

    @DeleteMapping("/course/delete/{id}")
    public Result<?> deleteCourse(@PathVariable Integer id,
                                   @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        if (user != null && user.getRole() == 1) {
            Course existing = courseService.getById(id);
            if (existing != null && !existing.getTeacherId().equals(user.getId())) {
                throw new com.edu.course.common.BusinessException("只能删除自己教授的课程");
            }
        }
        courseService.deleteCourse(id);
        return Result.success();
    }

    @PutMapping("/course/disable/{id}")
    public Result<?> disableCourse(@PathVariable Integer id,
                                    @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        if (user != null && user.getRole() == 1) {
            Course existing = courseService.getById(id);
            if (existing != null && !existing.getTeacherId().equals(user.getId())) {
                throw new com.edu.course.common.BusinessException("只能停用自己教授的课程");
            }
        }
        courseService.toggleStatus(id, 0);
        return Result.success();
    }

    @PutMapping("/course/enable/{id}")
    public Result<?> enableCourse(@PathVariable Integer id,
                                   @RequestHeader(value = "Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = userService.getUserByUsername(jwtUtil.getUsernameFromToken(token));
        if (user != null && user.getRole() == 1) {
            Course existing = courseService.getById(id);
            if (existing != null && !existing.getTeacherId().equals(user.getId())) {
                throw new com.edu.course.common.BusinessException("只能启用自己教授的课程");
            }
        }
        courseService.toggleStatus(id, 1);
        return Result.success();
    }

    // ==================== 角色权限管理 ====================

    @GetMapping("/roles")
    public Result<List<Role>> getRoles() {
        return Result.success(roleService.getAllRoles());
    }

    @PostMapping("/role/add")
    public Result<?> addRole(@RequestBody Role role) {
        roleService.addRole(role);
        return Result.success();
    }

    @PutMapping("/role/update")
    public Result<?> updateRole(@RequestBody Role role) {
        roleService.updateRole(role);
        return Result.success();
    }

    @DeleteMapping("/role/delete/{id}")
    public Result<?> deleteRole(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/role/menus/{roleId}")
    public Result<List<Integer>> getRoleMenuIds(@PathVariable Integer roleId) {
        return Result.success(roleService.getRoleMenuIds(roleId));
    }

    @PostMapping("/role/menus/{roleId}")
    public Result<?> saveRoleMenus(@PathVariable Integer roleId, @RequestBody List<Integer> menuIds) {
        roleService.saveRoleMenus(roleId, menuIds);
        return Result.success();
    }

    @GetMapping("/role/all-menus")
    public Result<List<Menu>> getAllMenusForRole() {
        return Result.success(roleService.getAllEnabledMenus());
    }

    // ==================== 学生管理 ====================

    @GetMapping("/students")
    public Result<List<User>> getStudents(@RequestParam(required = false) String keyword) {
        return Result.success(userService.getStudentList(keyword));
    }

    @PutMapping("/student/disable/{id}")
    public Result<?> disableStudent(@PathVariable Integer id) {
        userService.toggleDisabled(id, 1);
        return Result.success();
    }

    @PutMapping("/student/enable/{id}")
    public Result<?> enableStudent(@PathVariable Integer id) {
        userService.toggleDisabled(id, 0);
        return Result.success();
    }

    @PutMapping("/student/update")
    public Result<?> updateStudent(@RequestBody Map<String, String> params) {
        Integer id = Integer.parseInt(params.get("id"));
        String realName = params.get("realName");
        String email = params.get("email");
        String phone = params.get("phone");
        userService.updateStudentInfo(id, realName, email, phone);
        return Result.success();
    }

    @PutMapping("/student/reset-pwd/{id}")
    public Result<?> resetStudentPwd(@PathVariable Integer id) {
        userService.resetStudentPassword(id);
        return Result.success();
    }
}
