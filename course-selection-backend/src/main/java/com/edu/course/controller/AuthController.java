package com.edu.course.controller;

import com.edu.course.common.BusinessException;
import com.edu.course.common.Result;
import com.edu.course.dto.LoginRequest;
import com.edu.course.entity.Menu;
import com.edu.course.entity.User;
import com.edu.course.service.RoleService;
import com.edu.course.service.UserService;
import com.edu.course.service.MailService;
import com.edu.course.service.MailProducer;
import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器 — 处理登录/用户信息/密码修改/动态菜单
 * 所有接口在 /api/auth/** 下，SecurityConfig中已放行(permitAll)
 */
@RestController  // = @Controller + @ResponseBody: 所有方法返回JSON
@RequestMapping("/api/auth")  // 统一前缀
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;  // Spring Security的认证管理器(在SecurityConfig中暴露为Bean)
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @Autowired
    private MailService mailService;

    @Autowired
    private MailProducer mailProducer;  // RabbitMQ 异步邮件生产者  // 邮件服务（教师忘记密码申请时给管理员发邮件）

    /** 接收教师密码重置申请的管理员邮箱（通过配置文件 mail.admin-email 配置） */
    @org.springframework.beans.factory.annotation.Value("${mail.admin-email}")
    private String adminEmail;

    /**
     * 用户登录 — 整个系统的入口
     * 
     * 流程:
     *   ① AuthenticationManager.authenticate() → 调CustomUserDetailsService.loadUserByUsername()
     *   ② 从DB加载用户+权限 → BCrypt.matches()验证密码
     *   ③ 认证成功生成JWT Token(含userId/role/username)
     *   ④ 返回Token给前端 → sessionStorage存储 → 后续请求带Authorization头
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {  // @RequestBody: 将JSON转为LoginRequest对象
        // ① Spring Security认证: 失败会抛AuthenticationException(由GlobalExceptionHandler转为500)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())  // 未认证的Token
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);  // 设置当前线程的安全上下文

        // ② 获取认证成功的用户详情(UserDetails包含用户名密码和权限列表)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userService.getUserByUsername(request.getUsername());  // 再查一次数据库拿完整User对象(含role/id)

        // ③ 提取角色编码(去掉ROLE_前缀，如ROLE_ADMIN→ADMIN)
        String role = authentication.getAuthorities().iterator().next()  // 取第一个权限(就是ROLE_xxx)
                .getAuthority().replace("ROLE_", "");  // 去掉Spring Security的ROLE_前缀

        // ④ 生成JWT: 将userId/role/username编码进Token
        String token = jwtUtil.generateToken(user.getId(), role, user.getUsername());

        // ⑤ 构建响应: 前端Login.vue拿到后存sessionStorage
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("token", token);            // JWT Token(后续请求的凭证)
        resultMap.put("userId", user.getId());    // 用户ID
        resultMap.put("username", user.getUsername());  // 登录账号
        resultMap.put("realName", user.getRealName());  // 真实姓名(页面显示)
        resultMap.put("role", role);              // 角色(ADMIN/TEACHER/STUDENT，路由跳转依据)

        return Result.success(resultMap);
    }

    /**
     * 获取当前用户信息 — 用于前端刷新或验证Token有效性
     * 直接从头解析Token(不走Security认证流程，因为/auth已放行)
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 去掉"Bearer "前缀
        String username = jwtUtil.getUsernameFromToken(token);  // 从Token提取用户名
        User user = userService.getUserByUsername(username);    // 查数据库获取完整信息

        Map<String, Object> infoMap = new HashMap<>();
        infoMap.put("userId", user.getId());
        infoMap.put("username", user.getUsername());
        infoMap.put("realName", user.getRealName());
        infoMap.put("role", user.getRole());  // 这里是Integer role(0/1/2)，前端自行转换
        infoMap.put("email", user.getEmail());

        return Result.success(infoMap);
    }

    /**
     * 修改密码 — 需提供旧密码验证
     * UserService.changePassword()内部: BCrypt.matches(旧密码明文, DB密文) → 验证通过 → encode(新密码) → update
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> params,
                                     @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Integer userId = jwtUtil.getUserIdFromToken(token);  // 从Token提取userId，防止用户改别人的密码
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success();  // 无data返回，前端收到code=200即弹窗提示成功
    }

    /**
     * 忘记密码 — 无需登录，通过账号+手机号验证身份后重置为123456
     * 注意: 此接口在 /api/auth/** 下已放行，任何人都可以调用
     */
    @PostMapping("/forgot-password")
    public Result<?> forgotPassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String phone = params.get("phone");
        userService.resetPasswordByPhone(username, phone);  // 校验账号+手机号匹配后重置
        return Result.success();
    }

    /**
     * 教师忘记密码 — 提交申请，由管理员审核后手动重置
     * 与学生的自助重置不同：教师输入账号+手机号 → 系统发邮件给管理员 → 管理员在后台手动重置
     */
    @PostMapping("/forgot-password-teacher")
    public Result<?> forgotPasswordTeacher(@RequestBody Map<String, String> params) {
        String username = params.get("username");   // 教师账号（前端表单字段名就是username）
        String phone = params.get("phone");          // 预留手机号

        // 查用户：必须是教师（role=1）
        User user = userService.getUserByUsername(username);
        if (user == null) {
            throw new BusinessException("教师账号不存在");
        }
        if (user.getRole() != 1) {  // 不是教师
            throw new BusinessException("该账号不是教师账号");
        }
        if (user.getPhone() == null || !user.getPhone().equals(phone)) {
            throw new BusinessException("手机号与预留手机号不匹配");
        }

        // 发邮件给管理员
        String subject = "【选课系统】教师密码重置申请";
        String content = "管理员您好：\n\n"
                + "有一位教师提交了密码重置申请，信息如下：\n\n"
                + "  教师账号：" + username + "\n"
                + "  姓名：" + user.getRealName() + "\n"
                + "  手机号：" + phone + "\n"
                + "  申请时间：" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + "\n\n"
                + "请在后台管理中为该教师重置密码。\n\n"
                + "此邮件由系统自动发送，请勿回复。";
        mailProducer.sendMail(adminEmail, subject, content);  // 通过 RabbitMQ 异步发送

        return Result.success("申请已提交，请联系管理员处理");  // 前端展示这段文字
    }

    /**
     * 获取当前用户有权访问的动态菜单 — Layout.vue 侧边栏渲染用
     * 
     * 管理员/教师: 从 role_menu 表查分配的菜单 → 与硬编码基础菜单合并显示
     * 学生: 使用硬编码的4个菜单(选课中心/我的选课/公告/资料)
     */
    @GetMapping("/menus")
    public Result<List<Menu>> getMyMenus(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtil.getUsernameFromToken(token);
        User user = userService.getUserByUsername(username);
        if (user == null) return Result.success(java.util.Collections.emptyList());
        List<Menu> menus = roleService.getMenusByUserRole(user.getRole());  // 从role_menu+menu表联查
        return Result.success(menus);
    }
}
