package com.edu.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.course.entity.Message;
import com.edu.course.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

    /** 获取某用户的所有消息（按时间倒序） */
    public List<Message> getMessagesByUser(Integer userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .orderByDesc(Message::getCreateTime);  // 最新消息在前
        return messageMapper.selectList(wrapper);
    }

    /** 获取某用户的未读消息数（供前端红点徽章使用） */
    public long getUnreadCount(Integer userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0);  // 只统计未读
        return messageMapper.selectCount(wrapper);
    }

    /** 标记消息为已读 */
    public void markAsRead(Integer messageId) {
        Message msg = messageMapper.selectById(messageId);
        if (msg != null && msg.getIsRead() == 0) {
            msg.setIsRead(1);
            messageMapper.updateById(msg);
        }
    }

    /** 发送消息（给指定用户） */
    public void sendMessage(Integer receiverId, String title, String content) {
        Message msg = new Message();
        msg.setReceiverId(receiverId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setIsRead(0);           // 默认为未读
        msg.setCreateTime(new Date());
        messageMapper.insert(msg);
    }
}
