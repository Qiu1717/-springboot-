package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.common.BusinessException;
import com.edu.course.entity.Course;
import com.edu.course.entity.CourseFile;
import com.edu.course.entity.User;
import com.edu.course.mapper.CourseFileMapper;
import com.edu.course.mapper.CourseMapper;
import com.edu.course.mapper.SelectionMapper;
import com.edu.course.mapper.UserMapper;
import com.edu.course.dto.SelectionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程资料服务 — 文件上传/下载/删除 + 角色权限控制
 */
@Service
public class CourseFileService {

    @Autowired private CourseFileMapper courseFileMapper;
    @Autowired private UserMapper userMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private SelectionMapper selectionMapper;

    @Value("${file.upload-dir}")  // E:/springboot课设项目/uploads
    private String uploadDir;

    /**
     * 上传文件 — @Transactional保证文件存盘+数据库记录原子性
     * 
     * @param file       Spring MVC自动绑定的上传文件
     * @param uploaderId 上传者ID(从JWT Token中解析)
     * @param courseId   目标课程ID
     */
    @Transactional
    public CourseFile uploadFile(MultipartFile file, Integer uploaderId, Integer courseId) throws IOException {
        if (file.isEmpty()) throw new BusinessException("文件为空");

        User user = userMapper.selectById(uploaderId);
        if (user == null) throw new BusinessException("用户不存在");

        // 权限校验: 教师(user.role=1)只能给自己教的课上传资料
        if (user.getRole() == 1) {
            Course course = courseMapper.selectById(courseId);
            if (course == null || !course.getTeacherId().equals(uploaderId)) {
                throw new BusinessException("只能给自己教授的课程上传资料");
            }
        }

        // ===== 文件存储 =====
        String originalName = file.getOriginalFilename();  // 原始文件名(如 "课件.pptx")
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();  // 提取扩展名如 .pptx
        }
        String storedName = UUID.randomUUID().toString() + ext;  // UUID重命名防止文件名冲突(如 a1b2c3.pptx)
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();  // 目录不存在则创建
        file.transferTo(Paths.get(uploadDir, storedName).toFile());  // 将上传文件写入磁盘

        // ===== 数据库记录 =====
        CourseFile cf = new CourseFile();
        cf.setFileName(originalName);       // 记录原始文件名(下载时还原)
        cf.setFileSize(file.getSize());     // 文件大小(字节)
        cf.setFileExtension(ext);           // 扩展名
        cf.setFilePath(storedName);         // 服务器存储名(UUID)
        cf.setUploaderId(uploaderId);       // 上传者ID
        cf.setUploaderName(user.getRealName());  // 上传者姓名
        cf.setUploaderRole(user.getRole());      // 上传者角色(0管理员/1教师)
        cf.setCourseId(courseId);           // 关联课程
        cf.setUploadTime(new Date());       // 上传时间
        courseFileMapper.insert(cf);
        return cf;  // 返回存入的记录(含自增ID)
    }

    /**
     * 获取某课程的文件列表
     */
    public List<CourseFile> getFilesByCourse(Integer courseId) {
        return courseFileMapper.selectList(
            new LambdaQueryWrapper<CourseFile>()
                .eq(CourseFile::getCourseId, courseId)
                .orderByDesc(CourseFile::getUploadTime)
        );
    }

    /**
     * 获取当前用户有权限看到的课程列表(角色差异化)
     * 
     * 管理员(role=0): 所有启用课程
     * 教师(role=1): 自己教授的课程
     * 学生(role=2): 已选课程(通过selection表关联)
     */
    public List<Course> getMyCourses(Integer userId, Integer role) {
        if (role == 0) {
            return courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1)  // 管理员: 全部启用课程
            );
        } else if (role == 1) {
            return courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                    .eq(Course::getTeacherId, userId)  // 教师: 自己教的
                    .eq(Course::getStatus, 1)          // 仅启用的
            );
        } else {
            // 学生: 先从selection表查已选课程的ID集合，再批量查课程信息
            List<SelectionVO> selections = selectionMapper.selectByStudentId(userId);
            if (selections.isEmpty()) return Collections.emptyList();
            Set<Integer> courseIds = selections.stream()
                .map(SelectionVO::getCourseId).collect(Collectors.toSet());  // 提取去重的课程ID集合
            return courseMapper.selectBatchIds(new ArrayList<>(courseIds)).stream()  // 批量查课程
                .filter(c -> c.getStatus() == 1)  // 仅返回启用的(可能有些课已被管理员停用)
                .collect(Collectors.toList());
        }
    }

    /** 根据ID获取文件 */
    public CourseFile getById(Integer id) {
        return courseFileMapper.selectById(id);
    }

    /**
     * 删除文件 — 权限控制: 管理员全删，教师只能删自己上传的
     * Files.deleteIfExists: 先删磁盘文件(失败忽略)，再删数据库记录
     */
    public void deleteFile(Integer fileId, Integer userId, Integer userRole) {
        CourseFile cf = courseFileMapper.selectById(fileId);
        if (cf == null) throw new BusinessException("文件不存在");
        if (userRole != 0 && !cf.getUploaderId().equals(userId)) {  // 非管理员 且 不是自己的文件
            throw new BusinessException("只能删除自己上传的文件");
        }
        try { Files.deleteIfExists(Paths.get(uploadDir, cf.getFilePath())); } catch (IOException ignored) {}  // 删除磁盘文件
        courseFileMapper.deleteById(fileId);  // 删除数据库记录
    }
}
