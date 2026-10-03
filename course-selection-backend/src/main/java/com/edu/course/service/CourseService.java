package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.entity.Course;
import com.edu.course.entity.User;
import com.edu.course.mapper.CourseMapper;
import com.edu.course.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程服务
 */
@Slf4j
@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 填充教师姓名 — 私有工具方法
     * 遍历课程列表，根据teacherId查user表获取realName
     * 存入 @TableField(exist=false) 的 teacherName 字段(仅展示用)
     */
    private void fillTeacherName(List<Course> courses) {
        for (Course c : courses) {
            if (c.getTeacherId() != null) {
                User teacher = userMapper.selectById(c.getTeacherId());  // 逐条查询(可优化为批量)
                c.setTeacherName(teacher != null ? teacher.getRealName() : "未知");
            }
        }
    }

    /** 根据ID查课程 — @Cacheable: 结果存入Spring Cache(course::id)，下次查询直接返回缓存 */
    @Cacheable(value = "course", key = "#id")
    public Course getById(Integer id) {
        log.info("🔴 [Redis] 缓存未命中 course::{}，从 MySQL 查询并写入 Redis", id);
        return courseMapper.selectById(id);
    }

    /**
     * 获取可选课程 — 学生选课中心使用（带缓存，10分钟自动过期由 Redis TTL 控制）
     */
    @Cacheable(value = "availableCourses")
    public List<Course> getAvailableCourses() {
        log.info("🔴 [Redis] 缓存未命中 availableCourses，从 MySQL 查询并写入 Redis");
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getStatus, 1)
                .apply("selected < capacity");
        return courseMapper.selectList(wrapper);
    }

    /**
     * 获取所有课程
     */
    public List<Course> getAllCourses() {
        List<Course> list = courseMapper.selectList(null);
        fillTeacherName(list);
        return list;
    }

    /**
     * 根据教师ID查询课程
     */
    public List<Course> getCoursesByTeacher(Integer teacherId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getTeacherId, teacherId);
        List<Course> list = courseMapper.selectList(wrapper);
        fillTeacherName(list);
        return list;
    }

    /**
     * 获取满员课程列表
     */
    public List<Course> getFullCourses() {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.apply("selected >= capacity");
        return courseMapper.selectList(wrapper);
    }

    /**
     * 新增课程
     */
    @CacheEvict(value = {"course", "availableCourses"}, allEntries = true)
    public void addCourse(Course course) {
        course.setSelected(0);
        courseMapper.insert(course);
        log.info("🔴 [Redis] 清除缓存 availableCourses（原因：新增课程「{}」）", course.getName());
    }

    @CacheEvict(value = {"course", "availableCourses"}, allEntries = true)
    public void updateCourse(Course course) {
        courseMapper.updateById(course);
        log.info("🔴 [Redis] 清除缓存 course + availableCourses（原因：更新课程「{}」）", course.getName());
    }

    /**
     * 删除课程
     */
    @CacheEvict(value = {"course", "availableCourses"}, allEntries = true)
    public void deleteCourse(Integer id) {
        courseMapper.deleteById(id);
        log.info("🔴 [Redis] 清除缓存 course + availableCourses（原因：删除课程 id={}）", id);
    }

    /**
     * 切换课程启用状态
     */
    @CacheEvict(value = {"course", "availableCourses"}, allEntries = true)
    public void toggleStatus(Integer id, Integer status) {
        Course course = courseMapper.selectById(id);
        if (course != null) {
            course.setStatus(status);
            courseMapper.updateById(course);
            log.info("🔴 [Redis] 清除缓存 course + availableCourses（原因：{}课程「{}」）",
                    status == 0 ? "停用" : "启用", course.getName());
        }
    }
}
