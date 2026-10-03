package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.entity.Course;
import com.edu.course.entity.CourseFile;
import com.edu.course.service.CourseFileService;
import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件控制器 - 课程资料上传/下载/列表/删除
 */
@RestController
@RequestMapping("/api/files")
public class FilesController {

    @Autowired private CourseFileService fileService;
    @Autowired private JwtUtil jwtUtil;
    @Value("${file.upload-dir}") private String uploadDir;

    /** 获取我的课程（管理员=全部，教师=自己教的，学生=已选的） */
    @GetMapping("/my-courses")
    public Result<List<Course>> myCourses(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String roleStr = jwtUtil.getRoleFromToken(token);
        int role = "ADMIN".equals(roleStr) ? 0 : ("TEACHER".equals(roleStr) ? 1 : 2);
        return Result.success(fileService.getMyCourses(userId, role));
    }

    /** 上传文件 */
    @PostMapping("/upload")
    public Result<CourseFile> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam Integer courseId,
                                      @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        try {
            return Result.success(fileService.uploadFile(file, userId, courseId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取某课程的文件列表 */
    @GetMapping("/by-course/{courseId}")
    public Result<List<CourseFile>> listByCourse(@PathVariable Integer courseId) {
        return Result.success(fileService.getFilesByCourse(courseId));
    }

    /** 下载文件 */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Integer id) {
        CourseFile cf = fileService.getById(id);
        if (cf == null) return ResponseEntity.notFound().build();
        File f = new File(uploadDir, cf.getFilePath());
        if (!f.exists()) return ResponseEntity.notFound().build();
        String encodedName = URLEncoder.encode(cf.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(new FileSystemResource(f));
    }

    /** 删除文件 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Integer id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String roleStr = jwtUtil.getRoleFromToken(token);
        int userRole = "ADMIN".equals(roleStr) ? 0 : 1;
        fileService.deleteFile(id, userId, userRole);
        return Result.success();
    }
}
