-- ====================================================
-- 学生选课管理系统 - 数据库初始化脚本
-- ====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS course_selection
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE course_selection;

-- ====================================================
-- 1. 用户表（包含学生、教师、管理员）
-- ====================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `role` TINYINT NOT NULL COMMENT '角色：0管理员，1教师，2学生',
  `email` VARCHAR(100) COMMENT '邮箱（用于通知）',
  `phone` VARCHAR(20) COMMENT '手机号（忘记密码时验证身份）',
  `disabled` TINYINT DEFAULT 0 COMMENT '是否禁用：0正常，1禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ====================================================
-- 2. 课程表
-- ====================================================
CREATE TABLE IF NOT EXISTS `course` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `credit` FLOAT NOT NULL COMMENT '学分',
  `teacher_id` INT NOT NULL COMMENT '教师ID（关联user.id）',
  `capacity` INT NOT NULL COMMENT '总容量',
  `selected` INT DEFAULT 0 COMMENT '已选人数',
  `term` VARCHAR(20) NOT NULL COMMENT '学期，如2025-2026-1',
  `schedule` VARCHAR(200) COMMENT '上课时间地点',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1启用，0禁用',
  FOREIGN KEY (`teacher_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- ====================================================
-- 3. 选课记录表
-- ====================================================
CREATE TABLE IF NOT EXISTS `selection` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `student_id` INT NOT NULL,
  `course_id` INT NOT NULL,
  `score` FLOAT DEFAULT NULL COMMENT '成绩',
  `select_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_student_course` (`student_id`, `course_id`),
  FOREIGN KEY (`student_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`course_id`) REFERENCES `course`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课记录表';

-- ====================================================
-- 4. 公告表
-- ====================================================
CREATE TABLE IF NOT EXISTS `notice` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `content` TEXT NOT NULL,
  `author` VARCHAR(50) COMMENT '发布者（管理员落款"教务处"，教师落款真实姓名）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ====================================================
-- 5. 菜单表（用于RBAC）
-- ====================================================
CREATE TABLE IF NOT EXISTS `menu` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `path` VARCHAR(200) COMMENT '前端路由',
  `permission` VARCHAR(100) COMMENT '权限标识',
  `parent_id` INT DEFAULT 0,
  `sort` INT DEFAULT 0,
  `disabled` TINYINT DEFAULT 0 COMMENT '0启用，1禁用'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- ====================================================
-- 6. 角色表
-- ====================================================
CREATE TABLE IF NOT EXISTS `role` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `code` VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ====================================================
-- 7. 角色菜单关联表
-- ====================================================
CREATE TABLE IF NOT EXISTS `role_menu` (
  `role_id` INT,
  `menu_id` INT,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ====================================================
-- 8. 课程资料表
-- ====================================================
CREATE TABLE IF NOT EXISTS `course_file` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_size` BIGINT COMMENT '文件大小（字节）',
  `file_extension` VARCHAR(20) COMMENT '文件扩展名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '服务器存储相对路径',
  `uploader_id` INT COMMENT '上传者ID',
  `uploader_name` VARCHAR(50) COMMENT '上传者姓名',
  `uploader_role` TINYINT COMMENT '上传者角色：0管理员 1教师',
  `course_id` INT NOT NULL COMMENT '关联课程ID',
  `upload_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`uploader_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`course_id`) REFERENCES `course`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程资料表';

-- ====================================================
-- 9. 站内消息表
-- ====================================================
CREATE TABLE IF NOT EXISTS `message` (
  `id` INT PRIMARY KEY AUTO_INCREMENT,
  `receiver_id` INT NOT NULL COMMENT '接收者用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息正文',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读：0未读，1已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`receiver_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ====================================================
-- 初始化数据
-- 密码都是 123456 的BCrypt加密结果
-- ====================================================

-- 插入用户（管理员/教师/学生）
-- 密码均为 123456，BCrypt加密结果
-- phone 为占位演示值，正式使用请自行修改
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `email`, `phone`) VALUES
('admin', '$2a$10$cR7YjRQ/p8UHQrXq/KIhee2I/MiFI3jOaTX5S0he5ZHkka3VIQjqm', '系统管理员', 0, 'admin@example.com', '13800000000'),
('teacher1', '$2a$10$cR7YjRQ/p8UHQrXq/KIhee2I/MiFI3jOaTX5S0he5ZHkka3VIQjqm', '张老师', 1, 'teacher1@example.com', '13800000001'),
('teacher2', '$2a$10$cR7YjRQ/p8UHQrXq/KIhee2I/MiFI3jOaTX5S0he5ZHkka3VIQjqm', '王老师', 1, 'teacher2@example.com', '13800000002'),
('student1', '$2a$10$cR7YjRQ/p8UHQrXq/KIhee2I/MiFI3jOaTX5S0he5ZHkka3VIQjqm', '李学生', 2, 'student1@example.com', '13800000003'),
('student2', '$2a$10$cR7YjRQ/p8UHQrXq/KIhee2I/MiFI3jOaTX5S0he5ZHkka3VIQjqm', '赵学生', 2, 'student2@example.com', '13800000004');

-- 插入课程
INSERT INTO `course` (`name`, `credit`, `teacher_id`, `capacity`, `selected`, `term`, `schedule`) VALUES
('Java程序设计', 3.0, 2, 50, 0, '2025-2026-1', '周一 1-2节 机房A101'),
('数据库原理', 2.5, 2, 40, 0, '2025-2026-1', '周二 3-4节 教学楼B202'),
('软件工程', 3.0, 3, 45, 0, '2025-2026-1', '周三 1-2节 教学楼C303'),
('计算机网络', 2.0, 3, 60, 0, '2025-2026-1', '周四 5-6节 机房A102'),
('操作系统', 3.5, 2, 35, 0, '2025-2026-1', '周五 3-4节 教学楼B201');

-- 插入公告
INSERT INTO `notice` (`title`, `content`, `author`) VALUES
('欢迎使用选课系统', '本学期选课时间为2025年9月1日至9月10日，请同学们按时完成选课。如有疑问请联系教务处。', '教务处'),
('选课须知', '每位同学每学期至少选修2门课程，最多选修5门课程。选课结束后不可更改，请慎重选择。', '教务处');

-- 插入菜单（permission 需与代码中的权限码一致）
INSERT INTO `menu` (`name`, `path`, `permission`, `parent_id`, `sort`, `disabled`) VALUES
('系统管理', '/admin', 'admin:menu', 0, 1, 0),
('菜单管理', '/admin/menu', 'admin:menu:manage', 1, 1, 0),
('公告管理', '/admin/notice', 'admin:notice:manage', 1, 2, 0),
('教师管理', '/admin/teacher', 'admin:teacher:manage', 1, 3, 0),
('学生管理', '/admin/student', 'admin:student:manage', 1, 4, 0),
('课程管理', '/admin/course', 'admin:course:manage', 1, 5, 0),
('角色权限', '/admin/role', 'admin:role:manage', 1, 6, 0);

-- 插入角色
INSERT INTO `role` (`name`, `code`) VALUES
('管理员', 'ROLE_ADMIN'),
('教师', 'ROLE_TEACHER'),
('学生', 'ROLE_STUDENT');

-- 分配角色菜单权限
-- 管理员：拥有全部菜单
INSERT INTO `role_menu` (`role_id`, `menu_id`) VALUES
(1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(1,7);

-- 教师：公告管理 + 课程管理（可发布/维护自己的课程与公告）
INSERT INTO `role_menu` (`role_id`, `menu_id`) VALUES
(2,3),(2,6);

-- 学生：不分配后台菜单，仅使用前端固定的学生功能
