package com.edu.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edu.course.entity.Selection;
import com.edu.course.dto.SelectionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 选课记录Mapper — 唯一包含自定义SQL的Mapper
 * BaseMapper<Selection>: 继承MyBatis-Plus内置CRUD(selectList/insert/updateById/deleteById等)
 * 下面两个方法是自定义连表查询，因为需要JOIN course和user表获取课程名和学生名
 */
@Mapper
public interface SelectionMapper extends BaseMapper<Selection> {

    /**
     * 学生端: 查看自己的选课记录(连表查课程名/学分/学期)
     * 
     * LEFT JOIN: 即使course或user被删除也能查出selection记录
     * 返回 SelectionVO 而非 Selection: VO多出了 courseName/credit/term/studentName 展示字段
     */
    @Select("SELECT s.id, s.student_id, s.course_id, s.score, s.select_time, " +
            "c.name AS course_name, c.credit, c.term, " +       // 从course表取课程信息
            "u.real_name AS student_name " +                     // 从user表取学生姓名
            "FROM selection s " +
            "LEFT JOIN course c ON s.course_id = c.id " +       // 关联课程表
            "LEFT JOIN user u ON s.student_id = u.id " +        // 关联用户表
            "WHERE s.student_id = #{studentId}")                 // MyBatis占位符: 防SQL注入
    List<SelectionVO> selectByStudentId(@Param("studentId") Integer studentId);  // @Param: 指定参数名

    /**
     * 教师端: 按条件查询选课记录(学期/课程/学生姓名)
     * 
     * <script>标签: 让MyBatis解析其中的动态SQL标签(如<if>)
     * 三个条件都是可选的: 不传则不拼接到SQL中
     * LIKE CONCAT('%',#{},'%'): 模糊搜索学生姓名
     */
    @Select("<script>" +  // 包裹动态SQL
            "SELECT s.id, s.student_id, s.course_id, s.score, s.select_time, " +
            "c.name AS course_name, c.credit, c.term, " +
            "u.real_name AS student_name " +
            "FROM selection s " +
            "LEFT JOIN course c ON s.course_id = c.id " +
            "LEFT JOIN user u ON s.student_id = u.id " +
            "WHERE c.teacher_id = #{teacherId} " +  // 必须条件: 只查该教师的课
            "<if test='term != null and term != \"\"'> AND c.term = #{term} </if>" +  // 动态: 筛选学期
            "<if test='courseId != null'> AND s.course_id = #{courseId} </if>" +      // 动态: 筛选课程
            "<if test='studentName != null and studentName != \"\"'> AND u.real_name LIKE CONCAT('%',#{studentName},'%') </if>" +  // 动态: 模糊搜学生
            "</script>")
    List<SelectionVO> selectByCondition(@Param("teacherId") Integer teacherId,
                                        @Param("term") String term,
                                        @Param("courseId") Integer courseId,
                                        @Param("studentName") String studentName);
}
