package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.common.BusinessException;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.Course;
import com.edu.course.entity.Selection;
import com.edu.course.entity.User;
import com.edu.course.mapper.CourseMapper;
import com.edu.course.mapper.SelectionMapper;
import com.edu.course.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

/**
 * 选课服务 — 系统最核心的业务逻辑
 */
@Slf4j
@Service
public class SelectionService {

    @Autowired
    private SelectionMapper selectionMapper;  // 选课记录Mapper(含自定义连表SQL)
    @Autowired
    private CourseMapper courseMapper;        // 课程Mapper
    @Autowired
    private UserMapper userMapper;            // 用户Mapper
    @Autowired
    private MailService mailService;

    @Autowired
    private MailProducer mailProducer;  // RabbitMQ 异步邮件生产者          // 邮件服务(选课成功后通知学生)
    @Autowired
    private CacheManager cacheManager;  // 缓存管理器(用于 updateScore 手动清除 mySelections 缓存)

    /**
     * 学生选课 — @Transactional保证原子性: 插入记录+更新人数 要么全成功要么全回滚
     * 
     * 校验链(6步，任一步失败抛BusinessException回滚):
     *   ①是否已选过 ②选课数≤5 ③课程存在且启用 ④时间冲突 ⑤容量未满 ⑥通过后插入
     */
    @Transactional
    @CacheEvict(value = "mySelections", key = "#studentId")
    public void selectCourse(Integer studentId, Integer courseId) {
        // ① 检查重复选课: 同一个学生+同一门课只能有一条记录
        LambdaQueryWrapper<Selection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Selection::getStudentId, studentId)  // WHERE student_id = ?
                .eq(Selection::getCourseId, courseId);   // AND course_id = ?
        if (selectionMapper.selectCount(wrapper) > 0) {  // COUNT查询，比selectOne性能好
            throw new BusinessException("您已选过该课程");  // 抛异常→事务回滚→Spring返回500
        }

        // ② 检查选课数量上限: 每个学生最多选5门课
        LambdaQueryWrapper<Selection> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Selection::getStudentId, studentId);
        long selectedCount = selectionMapper.selectCount(countWrapper);
        if (selectedCount >= 5) {
            throw new BusinessException("选课已达上限（最多5门），请先退课再选");
        }

        // ②.① 检查课程是否存在且启用(status=1)
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == 0) {
            throw new BusinessException("课程不存在或已停用");
        }

        // ②.② 检查上课时间冲突: 遍历已选课程，比较schedule字段是否相同
        List<SelectionVO> mySelections = selectionMapper.selectByStudentId(studentId);
        for (SelectionVO s : mySelections) {
            Course existingCourse = courseMapper.selectById(s.getCourseId());
            if (existingCourse != null && existingCourse.getSchedule() != null
                    && course.getSchedule() != null
                    && existingCourse.getSchedule().equals(course.getSchedule())) {  // 字符串比较: 如"周一 1-2节 机房A101"
                throw new BusinessException("上课时间冲突！您已选课程「"
                        + existingCourse.getName() + "」占用了同一时段：" + course.getSchedule());
            }
        }

        // ③ 检查课程容量: selected < capacity 才可继续选
        if (course.getSelected() >= course.getCapacity()) {
            throw new BusinessException("课程已满员");  // 注意: 此处未加锁，高并发下可能超卖(生产环境应加乐观锁)
        }

        // ④ 插入选课记录(无成绩，score=null)
        Selection selection = new Selection();
        selection.setStudentId(studentId);
        selection.setCourseId(courseId);
        selection.setSelectTime(new Date());  // 当前时间
        selectionMapper.insert(selection);

        // ⑤ 更新课程已选人数(selected+1)
        course.setSelected(course.getSelected() + 1);
        courseMapper.updateById(course);

        // ⑥ 通过 RabbitMQ 异步发送邮件通知（不阻塞接口响应）
        User student = userMapper.selectById(studentId);
        if (student != null && student.getEmail() != null && !student.getEmail().isEmpty()) {
            mailProducer.sendMail(
                    student.getEmail(),
                    "选课成功通知",
                    "同学您好！您已成功选择课程：「" + course.getName() + "」，"
                            + "学分：" + course.getCredit() + "，"
                            + "学期：" + course.getTerm() + "。请按时上课！"
            );
        }
        log.info("🔴 [Redis] 清除缓存 mySelections::{}（原因：学生选课）", studentId);
    }

    /**
     * 更新成绩: 教师录入/修改单个学生成绩 → 清除该学生的选课缓存
     * 注: 方法参数只有 selectionId，无法用 @CacheEvict(key="#studentId")，
     *     因此通过 CacheManager 手动按 studentId 精准清除 mySelections 缓存
     */
    public void updateScore(Integer selectionId, Float score) {
        Selection selection = selectionMapper.selectById(selectionId);
        if (selection != null) {
            selection.setScore(score);
            selectionMapper.updateById(selection);
            // 手动清除该学生的选课缓存（学生端 my-selections 走 @Cacheable mySelections::studentId）
            org.springframework.cache.Cache cache = cacheManager.getCache("mySelections");
            if (cache != null) {
                cache.evict(selection.getStudentId());
            }
            log.info("🔴 [Redis] 清除缓存 mySelections::{}（原因：成绩更新）", selection.getStudentId());
        }
    }

    /** 查学生的选课记录(含课程名+学分+学期等，来自连表SQL) */
    @Cacheable(value = "mySelections", key = "#studentId")
    public List<SelectionVO> getSelectionsByStudentId(Integer studentId) {
        log.info("🔴 [Redis] 缓存未命中 mySelections::{}，从 MySQL 查询并写入 Redis", studentId);
        return selectionMapper.selectByStudentId(studentId);
    }

    /** 教师根据条件查询选课记录(学期/课程/学生姓名，动态SQL) */
    public List<SelectionVO> getSelectionsByCondition(Integer teacherId, String term,
                                                       Integer courseId, String studentName) {
        return selectionMapper.selectByCondition(teacherId, term, courseId, studentName);
    }

    /**
     * 退课 — 同样@Transactional保证原子性
     * 限制: 已有成绩(score不为null)的课不能退
     */
    @Transactional
    @CacheEvict(value = "mySelections", key = "#studentId")
    public void dropCourse(Integer studentId, Integer courseId) {
        LambdaQueryWrapper<Selection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Selection::getStudentId, studentId)
                .eq(Selection::getCourseId, courseId);
        Selection selection = selectionMapper.selectOne(wrapper);  // 查询唯一的选课记录

        if (selection == null) {
            throw new BusinessException("未找到该选课记录");
        }

        if (selection.getScore() != null) {  // 已有成绩→说明教师已录入，不能退
            throw new BusinessException("已有成绩，无法退课");
        }

        selectionMapper.deleteById(selection.getId());  // 物理删除选课记录

        // 恢复课程已选人数(selected-1，但不能小于0)
        Course course = courseMapper.selectById(courseId);
        if (course != null && course.getSelected() > 0) {
            course.setSelected(course.getSelected() - 1);
            courseMapper.updateById(course);
        }
        log.info("🔴 [Redis] 清除缓存 mySelections::{}（原因：退课）", studentId);
    }
}
