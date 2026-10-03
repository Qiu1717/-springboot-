package com.edu.course.security;

import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT认证过滤器 - 拦截请求并进行JWT认证
 */
@Component  // 标注为 Spring 组件，使其被自动扫描注册
public class JwtAuthenticationFilter extends OncePerRequestFilter {  // OncePerRequestFilter: 保证每次请求只执行一次过滤

    @Autowired
    private JwtUtil jwtUtil;  // JWT工具类：生成、解析、验证Token

    @Autowired
    private CustomUserDetailsService userDetailsService;  // 从数据库加载用户信息

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");  // 从HTTP请求头中取出 Authorization 字段

        if (authHeader != null && authHeader.startsWith("Bearer ")) {  // 检查是否带 Bearer 前缀
            String token = authHeader.substring(7);  // 去掉 "Bearer " 前缀(7个字符)，提取纯Token

            if (jwtUtil.validateToken(token)) {  // 验证Token是否有效(签名正确、未过期)
                String username = jwtUtil.getUsernameFromToken(token);  // 从Token中提取用户名
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);  // 查数据库加载用户和权限

                // 创建Spring Security认证对象，三个参数: 用户信息、密码(null因为已通过JWT验证)、权限列表
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));  // 附加请求详情(IP、SessionId等)

                SecurityContextHolder.getContext().setAuthentication(authentication);  // 将认证信息放入安全上下文，后续Controller可通过@AuthenticationPrincipal获取
            }
        }

        chain.doFilter(request, response);  // 无论是否认证成功都放行，由后续的权限注解(@PreAuthorize)决定是否拒绝
    }
}
