package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.dto.BatchScoreDTO;
import com.edu.course.dto.ScoreUpdateDTO;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.Course;
import com.edu.course.service.CourseService;
import com.edu.course.service.SelectionService;
import com.edu.course.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师控制器 - 课程查看、成绩录入
 */
@RestController
@RequestMapping("/api/teacher")
@PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
public class TeacherController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private SelectionService selectionService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前教师的课程列表
     */
    @GetMapping("/my-courses")
    public Result<List<Course>> getMyCourses(@AuthenticationPrincipal UserDetails userDetails) {
        Integer teacherId = userService.getUserIdByUsername(userDetails.getUsername());
        return Result.success(courseService.getCoursesByTeacher(teacherId));
    }

    /**
     * 查询选课记录（按学期、课程、学生姓名筛选）
     */
    @GetMapping("/selections")
    public Result<List<SelectionVO>> querySelections(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String studentName) {
        Integer teacherId = userService.getUserIdByUsername(userDetails.getUsername());
        List<SelectionVO> list = selectionService.getSelectionsByCondition(
                teacherId, term, courseId, studentName);
        return Result.success(list);
    }

    /**
     * 录入/更新成绩
     */
    @PutMapping("/score")
    public Result<?> updateScore(@RequestBody ScoreUpdateDTO dto) {
        selectionService.updateScore(dto.getSelectionId(), dto.getScore());
        return Result.success();
    }

    /**
     * 批量录入成绩
     */
    @PutMapping("/score/batch")
    public Result<?> batchUpdateScore(@RequestBody BatchScoreDTO dto) {
        if (dto.getScores() != null) {
            for (ScoreUpdateDTO s : dto.getScores()) {
                selectionService.updateScore(s.getSelectionId(), s.getScore());
            }
        }
        return Result.success();
    }
}
