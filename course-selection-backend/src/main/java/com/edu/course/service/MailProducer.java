package com.edu.course.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件消息生产者 — 把邮件投递到 RabbitMQ 队列（异步，不阻塞主线程）
 */
@Slf4j
@Component
public class MailProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMail(String to, String subject, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("to", to);
        msg.put("subject", subject);
        msg.put("content", content);

        rabbitTemplate.convertAndSend(
                com.edu.course.config.RabbitMQConfig.MAIL_EXCHANGE,
                com.edu.course.config.RabbitMQConfig.MAIL_ROUTING_KEY,
                msg
        );
        log.info("🐰 [RabbitMQ] 投递消息 → mail.exchange / mail.queue | to={} | subject={}", to, subject);
    }
}
