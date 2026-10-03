package com.edu.course.controller;

import com.edu.course.common.Result;
import com.edu.course.entity.Message;
import com.edu.course.service.MessageService;
import com.edu.course.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private JwtUtil jwtUtil;

    /** 获取当前用户的所有消息（从Token中解析userId） */
    @GetMapping
    public Result<List<Message>> getMessages(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        return Result.success(messageService.getMessagesByUser(userId));
    }

    /** 获取当前用户的未读消息数（供Layout.vue铃铛徽章用） */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        Integer userId = jwtUtil.getUserIdFromToken(token);
        return Result.success(messageService.getUnreadCount(userId));
    }

    /** 标记消息为已读 */
    @PutMapping("/{id}/read")
    public Result<?> markAsRead(@PathVariable Integer id) {
        messageService.markAsRead(id);
        return Result.success();
    }
}
