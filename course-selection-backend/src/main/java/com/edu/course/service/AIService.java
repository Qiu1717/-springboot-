package com.edu.course.service;

import com.edu.course.config.AIConfig;
import com.edu.course.dto.SelectionVO;
import com.edu.course.entity.Course;
import com.edu.course.entity.User;
import com.edu.course.mapper.CourseMapper;
import com.edu.course.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 智能体 — DeepSeek + 系统数据注入
 * 
 * 学生端：注入成绩/选课/学分数据，可分析成绩趋势、推荐课程
 * 教师端：注入所教课程+学生成绩数据，可分析班级分布
 * 管理员端：纯聊天模式，不注入数据（权限最低）
 */
@Slf4j
@Service
public class AIService {

    @Autowired private AIConfig aiConfig;
    @Autowired private RestTemplate restTemplate;
    @Autowired private UserMapper userMapper;
    @Autowired private SelectionService selectionService;
    @Autowired private CourseService courseService;

    public String chat(String userMessage, String currentPage, Integer userId) {
        User user = userMapper.selectById(userId);
        String roleName = getRoleName(user != null ? user.getRole() : null);
        Integer role = user != null ? user.getRole() : null;

        // 构建 System Prompt（含真实数据注入）
        String systemPrompt = buildSystemPrompt(currentPage, roleName, role, userId);

        return callDeepSeek(systemPrompt, userMessage);
    }

    private String getRoleName(Integer role) {
        if (role == null) return "访客";
        switch (role) { case 0: return "管理员"; case 1: return "教师"; case 2: return "学生"; default: return "用户"; }
    }

    /**
     * 构建 System Prompt — 核心：根据角色注入真实数据
     */
    private String buildSystemPrompt(String currentPage, String roleName, Integer role, Integer userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是「学生选课管理系统」的 AI 智能助手。当前用户是").append(roleName).append("。");
        prompt.append("请用中文回答，简洁专业，每次不超过300字。\n\n");

        // ===== 管理员端：纯聊天，不注入任何系统数据 =====
        if (role != null && role == 0) {
            prompt.append("【模式：基础助手】");
            prompt.append("你可以回答一般性问题，但不能查看学生成绩、课程详情等敏感数据。");
            prompt.append("如果用户问成绩或课程相关问题，请回复：'抱歉，管理员模式下不提供数据查询功能，请切换到教师或学生账号查看。'");
            return prompt.toString();
        }

        // ===== 学生端：注入选课+成绩数据 =====
        if (role != null && role == 2) {
            prompt.append("【模式：学生智能体 — 已注入真实数据】\n\n");

            // ① 注入可选课程列表
            List<Course> availableCourses = courseService.getAvailableCourses();
            if (!availableCourses.isEmpty()) {
                prompt.append("--- 可选课程列表（共").append(availableCourses.size()).append("门）---\n");
                for (Course c : availableCourses) {
                    String teacherName = c.getTeacherName() != null ? c.getTeacherName() : "未知";
                    prompt.append(String.format("· %s | 学分%.1f | 教师:%s | 容量:%d/%d | 学期:%s | 时间:%s\n",
                            c.getName(), c.getCredit(), teacherName,
                            c.getSelected(), c.getCapacity(), c.getTerm(), c.getSchedule()));
                }
                prompt.append("\n");
            }

            // ② 注入我的选课记录（含成绩）
            List<SelectionVO> mySelections = selectionService.getSelectionsByStudentId(userId);
            if (!mySelections.isEmpty()) {
                prompt.append("--- 我的选课记录（共").append(mySelections.size()).append("门）---\n");
                double totalCredits = 0;
                double totalScore = 0;
                int gradedCount = 0;
                for (SelectionVO s : mySelections) {
                    String scoreStr = s.getScore() != null ? String.format("%.1f分", s.getScore()) : "未出分";
                    prompt.append(String.format("· %s | 学分%.1f | 学期:%s | 成绩:%s\n",
                            s.getCourseName(), s.getCredit(), s.getTerm(), scoreStr));
                    if (s.getScore() != null && s.getCredit() != null) {
                        totalCredits += s.getCredit();
                        totalScore += s.getScore();
                        gradedCount++;
                    }
                }
                if (gradedCount > 0) {
                    prompt.append(String.format("\n📊 统计：已获学分 %.1f，已出分 %d 门，平均分 %.1f\n",
                            totalCredits, gradedCount, totalScore / gradedCount));
                }
                prompt.append("\n");
            }

            prompt.append("你可以：①根据可选课程推荐选课方案 ②分析我的成绩趋势 ③给出学习建议 ④回答课程相关问题。");
            prompt.append("回答时请引用具体数据（如'你的高数85分，英语92分...'）。");
            return prompt.toString();
        }

        // ===== 教师端：注入所教课程+学生数据 =====
        if (role != null && role == 1) {
            prompt.append("【模式：教师智能体 — 已注入真实数据】\n\n");

            List<Course> myCourses = courseService.getCoursesByTeacher(userId);
            if (!myCourses.isEmpty()) {
                prompt.append("--- 我教授的课程（共").append(myCourses.size()).append("门）---\n");
                for (Course c : myCourses) {
                    prompt.append(String.format("· ID=%d %s | 学分%.1f | 已选%d/%d | 学期:%s | 状态:%s\n",
                            c.getId(), c.getName(), c.getCredit(),
                            c.getSelected(), c.getCapacity(), c.getTerm(),
                            c.getStatus() == 1 ? "进行中" : "已停用"));
                }
                prompt.append("\n");

                // 注入每门课的学生成绩概要
                prompt.append("--- 各课程成绩概要 ---\n");
                for (Course c : myCourses) {
                    List<SelectionVO> selections = selectionService.getSelectionsByCondition(userId, null, c.getId(), null);
                    if (!selections.isEmpty()) {
                        long graded = selections.stream().filter(s -> s.getScore() != null).count();
                        double avg = selections.stream().filter(s -> s.getScore() != null)
                                .mapToDouble(SelectionVO::getScore).average().orElse(0);
                        long passed = selections.stream().filter(s -> s.getScore() != null && s.getScore() >= 60).count();
                        prompt.append(String.format("· %s：共%d人选修，已出分%d人，平均%.1f分，通过%d人\n",
                                c.getName(), selections.size(), graded, avg, passed));
                    }
                }
                prompt.append("\n");
            }

            prompt.append("你可以：①分析班级成绩分布 ②对比各课程选课率 ③给出教学建议 ④回答课程管理问题。");
            return prompt.toString();
        }

        prompt.append("请友好地回答用户的问题。");
        return prompt.toString();
    }

    /** 调用 DeepSeek Chat API（兼容 OpenAI 格式） */
    private String callDeepSeek(String systemPrompt, String userMessage) {
        String url = aiConfig.getApiUrl() + "/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("max_tokens", 600);
        requestBody.put("temperature", 0.7);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) return (String) message.get("content");
                }
            }
            return "AI 暂时无法回复，请稍后再试。";
        } catch (Exception e) {
            log.error("调用 DeepSeek API 失败: {}", e.getMessage());
            return "抱歉，AI 服务暂时不可用。请检查 API Key 是否正确配置。";
        }
    }
}
