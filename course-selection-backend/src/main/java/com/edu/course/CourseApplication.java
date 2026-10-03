package com.edu.course;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

/**
 * 学生选课管理系统 - 启动类
 */
@SpringBootApplication  // 组合注解: @Configuration + @EnableAutoConfiguration + @ComponentScan
@MapperScan("com.edu.course.mapper")  // 扫描MyBatis Mapper接口，替代每个Mapper上的@Mapper注解
@EnableCaching  // 启用Spring Cache(配合@Cacheable/@CacheEvict使用)
@EnableAsync  // 启用异步执行
@EnableScheduling  // 启用定时任务
public class CourseApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        // 启动后端服务
        SpringApplication.run(CourseApplication.class, args);
        
        System.out.println("========================================");
        System.out.println("  学生选课管理系统启动成功！");
        System.out.println("  后端 API:  http://localhost:8080");
        System.out.println("  前端页面:  http://localhost:3000");
        System.out.println("========================================");
        System.out.println("  正在启动前端服务...");
        
        // 等待后端完全启动
        Thread.sleep(3000);

        // 守护线程异步启动前端（用 start 命令在独立窗口运行，进程独立于 Java，不受后端停止影响）
        Thread frontendThread = new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "cmd", "/c", "start", "Frontend Server", "/min",
                    "E:\\springboot课设项目\\start-frontend-quiet.bat"
                );
                pb.start();
                System.out.println("  前端启动命令已发送");
            } catch (Exception e) {
                System.err.println("  前端启动失败: " + e.getMessage());
                System.err.println("  请手动运行: cd course-selection-front && npm run serve");
            }
        });
        frontendThread.setDaemon(true);
        frontendThread.start();

        // 等待前端启动命令执行
        Thread.sleep(5000);

        System.out.println("  前端正在编译，约 30 秒后可访问: http://localhost:3000");
        System.out.println("========================================");
    }
}
