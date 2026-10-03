package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.common.BusinessException;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.User;
import com.edu.course.mapper.SelectionMapper;
import com.edu.course.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;  // SecurityConfig中注册的BCryptPasswordEncoder

    /** 工具方法: 根据登录用户名查用户ID，Controller中频繁调用 */
    public Integer getUserIdByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        return user != null ? user.getId() : null;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    /**
     * 获取所有教师列表
     */
    public List<User> getTeacherList() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, 1); // 角色：1=教师
        return userMapper.selectList(wrapper);
    }

    /**
     * 切换用户禁用状态
     */
    public void toggleDisabled(Integer userId, Integer disabled) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setDisabled(disabled);
            userMapper.updateById(user);
        }
    }

    /** 新增教师: 前端传User对象 → Service设role=1 + BCrypt加密密码 */
    public void addTeacher(User teacher) {
        teacher.setRole(1);  // 强制设为教师角色(防止前端篡改role)
        teacher.setPassword(passwordEncoder.encode(teacher.getPassword()));  // BCrypt加密(不可逆)
        teacher.setDisabled(0);  // 新教师默认正常状态
        userMapper.insert(teacher);
    }

    /**
     * 修改密码
     */
    public void updatePassword(Integer userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
        }
    }

    /**
     * 修改密码（需验证旧密码） — 用户主动修改密码
     * passwordEncoder.matches(明文, 密文): 验证密码是否正确(BCrypt自带方法)
     */
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {  // BCrypt.matches: 从密文中提取salt来验证
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));  // 新密码也要BCrypt加密存储
        userMapper.updateById(user);
    }

    /**
     * 忘记密码 — 通过账号+手机号验证身份后重置为123456
     * 生产环境应发送验证码到手机，而非直接比对
     */
    public void resetPasswordByPhone(String username, String phone) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        if (user.getPhone() == null || !user.getPhone().equals(phone)) {  // 校验手机号是否匹配
            throw new BusinessException("手机号与账号不匹配");
        }
        user.setPassword(passwordEncoder.encode("123456"));  // 重置为默认密码123456
        userMapper.updateById(user);
    }

    /**
     * 管理员更新学生信息
     */
    public void updateStudentInfo(Integer id, String realName, String email, String phone) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("学生不存在");
        }
        if (user.getRole() != 2) {
            throw new BusinessException("该用户不是学生");
        }
        user.setRealName(realName);
        user.setEmail(email);
        user.setPhone(phone);
        userMapper.updateById(user);
    }

    /**
     * 管理员重置学生密码，重置为123456
     */
    public void resetStudentPassword(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("学生不存在");
        }
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
    }

    /**
     * 获取学生列表 — 支持关键词搜索(学号或姓名模糊匹配)
     * LambdaQueryWrapper.and(): 用括号包裹OR条件
     * SQL: WHERE role=2 AND (username LIKE '%kw%' OR real_name LIKE '%kw%')
     */
    public List<User> getStudentList(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, 2);  // 只查学生
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)  // OR: 学号模糊匹配
                    .or().like(User::getRealName, keyword));      // OR: 姓名模糊匹配
        }
        return userMapper.selectList(wrapper);
    }
}
