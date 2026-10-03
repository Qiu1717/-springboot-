package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.service.AIService;
import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 对话控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * AI 对话接口
     *
     * @param params: { message: "用户输入", currentPage: "/student/courses" }
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> params,
                                @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        String message = params.get("message");
        String currentPage = params.get("currentPage");

        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息不能为空");
        }

        String reply = aiService.chat(message, currentPage, userId);
        return Result.success(reply);
    }
}
