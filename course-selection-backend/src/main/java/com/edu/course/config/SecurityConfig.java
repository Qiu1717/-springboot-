package com.edu.course.config;

import com.edu.course.security.CustomUserDetailsService;
import com.edu.course.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 安全配置类
 */
@Configuration  // 标记为Spring配置类
@EnableWebSecurity  // 启用Spring Security的Web安全功能
@EnableGlobalMethodSecurity(prePostEnabled = true)  // 启用@PreAuthorize/@PostAuthorize注解，实现方法级权限控制
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private CustomUserDetailsService userDetailsService;  // 从数据库加载用户的Service

    @Autowired
    private JwtAuthenticationFilter jwtFilter;  // JWT认证过滤器(在UsernamePasswordFilter之前执行)

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // 使用BCrypt算法: 每次加密结果不同，自带salt，不可逆
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();  // 暴露AuthenticationManager为Bean，供AuthController.login()使用
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)  // 指定从哪个Service加载用户数据
                .passwordEncoder(passwordEncoder());  // 指定密码加密器(登录时用BCrypt.matches验证)
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()  // 启用CORS(使用CorsConfig中的配置)
                .csrf().disable()  // 禁用CSRF保护(前后端分离+无状态Session，不需要CSRF Token)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 不创建HttpSession，完全无状态(JWT替代)
                .and()
                .authorizeRequests()
                // 无需认证即可访问的路径
                .antMatchers("/api/auth/**", "/api/public/**", "/uploads/**", "/api/files/download/**", "/api/ai/**").permitAll()
                // 管理员接口: 仅需登录，细粒度权限由Controller上的@PreAuthorize控制
                .antMatchers("/api/admin/**").authenticated()
                // 教师接口: 仅需登录(TEACHER或ADMIN均可)
                .antMatchers("/api/teacher/**").authenticated()
                // 学生接口: 必须有ROLE_STUDENT角色
                .antMatchers("/api/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()  // 其他所有请求都需认证
                .and()
                // 将JWT过滤器插入UsernamePasswordAuthenticationFilter之前
                // 这样JWT先解析用户身份，UsernamePasswordFilter就不会再走表单登录流程
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
