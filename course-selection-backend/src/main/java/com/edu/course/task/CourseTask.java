package com.edu.course.task;

import com.edu.course.entity.Course;
import com.edu.course.entity.User;
import com.edu.course.mapper.UserMapper;
import com.edu.course.service.CourseService;
import com.edu.course.service.MailService;
import com.edu.course.service.MailProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 定时任务 - 课程满员预警、系统维护等
 */
@Slf4j
@Component
@EnableScheduling
public class CourseTask {

    @Autowired
    private CourseService courseService;

    @Autowired
    private MailService mailService;

    @Autowired
    private MailProducer mailProducer;  // RabbitMQ 异步邮件生产者

    @Autowired
    private UserMapper userMapper;

    /**
     * 每天凌晨1点执行：检查满员课程，发送预警邮件给对应教师
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkFullCourses() {
        log.info("========== 定时任务：检查满员课程 ==========");
        List<Course> fullCourses = courseService.getFullCourses();

        for (Course course : fullCourses) {
            User teacher = userMapper.selectById(course.getTeacherId());
            if (teacher != null && teacher.getEmail() != null && !teacher.getEmail().isEmpty()) {
                String subject = "课程满员预警 - 「" + course.getName() + "」";
                String content = "尊敬的" + teacher.getRealName() + "老师：\n\n"
                        + "您教授的课程「" + course.getName() + "」"
                        + "（学期：" + course.getTerm() + "）已选满（"
                        + course.getSelected() + "/" + course.getCapacity() + "），请及时关注。\n\n"
                        + "此邮件由系统自动发送，请勿回复。";
                mailProducer.sendMail(teacher.getEmail(), subject, content);  // 通过 RabbitMQ 异步发送
                log.info("已发送满员预警邮件 -> 教师: {}, 课程: {}", teacher.getRealName(), course.getName());
            }
        }
        log.info("========== 满员课程检查完毕，共 {} 门 ==========", fullCourses.size());
    }

    /**
     * 每周一上午8点执行：统计选课情况（示例）
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void weeklyReport() {
        log.info("========== 定时任务：周报统计 ==========");
        // 可以在此添加统计逻辑，如统计各课程选课率等
        log.info("========== 周报统计完成 ==========");
    }
}
