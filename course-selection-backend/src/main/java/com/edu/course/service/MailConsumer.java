package com.edu.course.service;

import com.edu.course.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 邮件消息消费者 — 从 mail.queue 取消息，真正发送邮件
 * acknowledge-mode: manual — 手动确认，发邮件成功才 ack，失败 nack 重试
 */
@Slf4j
@Component
public class MailConsumer {

    @Autowired
    private MailService mailService;

    @RabbitListener(queues = RabbitMQConfig.MAIL_QUEUE)
    public void handleMail(Map<String, String> msg, Channel channel,
                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String to = msg.get("to");
        String subject = msg.get("subject");
        String content = msg.get("content");

        log.info("🐰 [RabbitMQ] 收到消息，开始发送邮件 | to={} | subject={}", to, subject);

        try {
            mailService.sendSimpleMail(to, subject, content);
            channel.basicAck(deliveryTag, false);
            log.info("🐰 [RabbitMQ] 邮件发送成功，已确认(ack) | to={} | subject={}", to, subject);
        } catch (Exception e) {
            log.error("🐰 [RabbitMQ] 邮件发送失败，消息重新入队(nack) | to={} | error={}", to, e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                log.error("🐰 [RabbitMQ] nack 失败", ex);
            }
        }
    }
}
