package com.edu.course.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类 — 仅在启用redis缓存时加载
 * 
 * 当前 application.yml 中 spring.cache.type=simple(内存缓存)
 * 所以 @ConditionalOnProperty 条件不成立，此配置不会加载
 * 如需启用Redis: 将 spring.cache.type 改为 redis 并启动Redis服务
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")  // 只有当cache.type=redis时才生效
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);  // 注入Redis连接工厂
        // Key使用String序列化: 在Redis中可读
        template.setKeySerializer(new StringRedisSerializer());
        // Value使用JSON序列化: 对象自动转为JSON存储
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();  // 初始化(调用各Serializer的afterPropertiesSet)
        return template;
    }
}
