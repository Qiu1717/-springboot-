package com.edu.course.util;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 - 生成和解析Token
 */
@Slf4j  // Lombok: 自动生成log对象
@Component  // Spring管理的Bean，可通过@Autowired注入
public class JwtUtil {

    @Value("${jwt.secret}")  // 从application.yml读取JWT签名密钥
    private String secret;

    @Value("${jwt.expiration}")  // 从application.yml读取Token过期时间(毫秒)，默认86400000=24小时
    private Long expiration;

    /**
     * 生成JWT Token
     * @param userId 用户ID
     * @param role   角色编码（ADMIN/TEACHER/STUDENT）
     * @param username 用户名
     * @return JWT Token字符串
     */
    public String generateToken(Integer userId, String role, String username) {
        Map<String, Object> claims = new HashMap<>();  // JWT的Payload(载荷)，存储自定义数据
        claims.put("userId", userId);  // 存入用户ID，后续可从Token中取出
        claims.put("role", role);      // 存入角色(ADMIN/TEACHER/STUDENT)
        claims.put("username", username);  // 存入用户名
        return Jwts.builder()
                .setClaims(claims)  // 设置载荷
                .setExpiration(new Date(System.currentTimeMillis() + expiration))  // 设置过期时间: 当前时间+24小时
                .signWith(SignatureAlgorithm.HS512, secret)  // 使用HS512算法+密钥签名，防止篡改
                .compact();  // 压缩为URL安全的字符串(三段式: header.payload.signature)
    }

    /**
     * 从Token中获取用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Integer.class);
    }

    /**
     * 从Token中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("username", String.class);
    }

    /**
     * 解析Token的Claims(核心私有方法，其他get方法都调它)
     * 如果Token无效(过期/签名错误)，Jwts.parser()会抛出JwtException
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)  // 设置签名密钥(必须与签发时一致)
                .parseClaimsJws(token)  // 解析JWS(JSON Web Signature)
                .getBody();  // 获取Payload中的Claims
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);  // 尝试解析: 成功则Token有效
            return true;
        } catch (JwtException e) {  // 捕获所有JWT异常: 过期(ExpiredJwtException)、签名错误(SignatureException)、格式错误(MalformedJwtException)
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }
}
