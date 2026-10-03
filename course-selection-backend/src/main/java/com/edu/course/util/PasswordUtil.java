package com.edu.course.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类 — 开发辅助工具，非业务代码
 * 运行main方法: 输入明文密码 → 输出BCrypt密文(可复制到数据库user表)
 * 实际密码验证由SecurityConfig中的BCryptPasswordEncoder.matches()完成
 */
public class PasswordUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";  // 要加密的明文密码
        String encodedPassword = encoder.encode(rawPassword);  // BCrypt加密: 结果每执行一次都不同(随机salt)
        System.out.println("原始密码: " + rawPassword);
        System.out.println("BCrypt加密: " + encodedPassword);
        System.out.println("验证结果: " + encoder.matches(rawPassword, encodedPassword));  // 验证: 应为true
    }
}
