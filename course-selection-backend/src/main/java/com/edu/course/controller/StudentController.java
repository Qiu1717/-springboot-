package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.Course;
import com.edu.course.entity.Notice;
import com.edu.course.service.CourseService;
import com.edu.course.service.NoticeService;
import com.edu.course.service.SelectionService;
import com.edu.course.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生控制器 — 所有接口需要ROLE_STUDENT角色
 * @AuthenticationPrincipal: 从SecurityContext中获取当前登录用户(由JwtAuthFilter设置)
 */
@RestController
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")  // 类级别: 所有方法都需要STUDENT角色(Spring Security在方法执行前拦截)
public class StudentController {

    @Autowired private NoticeService noticeService;
    @Autowired private CourseService courseService;
    @Autowired private SelectionService selectionService;
    @Autowired private UserService userService;

    /** 获取公告列表 */
    @GetMapping("/notices")
    public Result<List<Notice>> getNotices() {
        return Result.success(noticeService.getAllNotices());
    }

    /** 获取可选课程列表（已启用且未满） */
    @GetMapping("/available-courses")
    public Result<List<Course>> getAvailableCourses() {
        return Result.success(courseService.getAvailableCourses());
    }

    /** 查看我的选课记录（含成绩） */
    @GetMapping("/my-selections")
    public Result<List<SelectionVO>> getMySelections(@AuthenticationPrincipal UserDetails userDetails) {
        Integer studentId = userService.getUserIdByUsername(userDetails.getUsername());
        return Result.success(selectionService.getSelectionsByStudentId(studentId));
    }

    /**
     * 选课 — 核心业务入口
     * @AuthenticationPrincipal: Spring Security自动注入当前登录用户，无需手动解析Token
     */
    @PostMapping("/select")
    public Result<?> selectCourse(@RequestParam Integer courseId,  // 前端通过query参数传 courseId
                                   @AuthenticationPrincipal UserDetails userDetails) {  // 当前登录用户
        Integer studentId = userService.getUserIdByUsername(userDetails.getUsername());  // username→ID转换
        selectionService.selectCourse(studentId, courseId);  // 6步校验+事务操作
        return Result.success();
    }

    /**
     * 退课 — 仅未出成绩的课程可退
     */
    @PostMapping("/drop")
    public Result<?> dropCourse(@RequestParam Integer courseId,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        Integer studentId = userService.getUserIdByUsername(userDetails.getUsername());
        selectionService.dropCourse(studentId, courseId);  // 校验+删除+恢复人数
        return Result.success();
    }

    /**
     * 选课统计 — 前端MySelections.vue顶部4张卡片的数据来源
     * 使用Java Stream API在内存中计算(数据量小时可行，数据量大应改为SQL聚合)
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@AuthenticationPrincipal UserDetails userDetails) {
        Integer studentId = userService.getUserIdByUsername(userDetails.getUsername());
        List<SelectionVO> list = selectionService.getSelectionsByStudentId(studentId);  // 查所有选课记录

        Map<String, Object> stats = new HashMap<>();
        int totalCourses = list.size();  // 总选课数
        long gradedCourses = list.stream().filter(s -> s.getScore() != null).count();  // 已出分课程数
        double totalCredits = list.stream()
                .filter(s -> s.getScore() != null && s.getCredit() != null)  // 只计算已出分课程的学分
                .mapToDouble(SelectionVO::getCredit).sum();  // 累加学分
        long passedCourses = list.stream().filter(s -> s.getScore() != null && s.getScore() >= 60).count();  // 通过数
        long failedCourses = list.stream().filter(s -> s.getScore() != null && s.getScore() < 60).count();  // 不及格数
        double totalScore = list.stream().filter(s -> s.getScore() != null).mapToDouble(SelectionVO::getScore).sum();
        double avgScore = gradedCourses > 0 ? Math.round(totalScore / gradedCourses * 10) / 10.0 : 0;  // 平均分(保留1位小数)

        stats.put("totalCourses", totalCourses);
        stats.put("totalCredits", totalCredits);
        stats.put("passedCourses", passedCourses);
        stats.put("failedCourses", failedCourses);
        stats.put("avgScore", avgScore);
        stats.put("gradedCourses", gradedCourses);

        return Result.success(stats);
    }
}
