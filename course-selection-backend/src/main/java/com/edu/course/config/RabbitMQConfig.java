package com.edu.course.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 — 声明邮件队列、交换机、绑定
 */
@Configuration
public class RabbitMQConfig {

    /** 邮件队列名 */
    public static final String MAIL_QUEUE = "mail.queue";
    /** 邮件交换机名 */
    public static final String MAIL_EXCHANGE = "mail.exchange";
    /** 路由键 */
    public static final String MAIL_ROUTING_KEY = "mail";

    /** 声明持久化队列（MQ 重启后队列不丢失） */
    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(MAIL_QUEUE).build();
    }

    /** 声明直连交换机 */
    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(MAIL_EXCHANGE);
    }

    /** 绑定队列到交换机，路由键 "mail" */
    @Bean
    public Binding mailBinding() {
        return BindingBuilder.bind(mailQueue()).to(mailExchange()).with(MAIL_ROUTING_KEY);
    }
}
