package com.edu.course.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * AI 配置类 — DeepSeek API 连接配置
 * DeepSeek API 兼容 OpenAI 格式: POST /v1/chat/completions
 */
@Configuration
public class AIConfig {

    @Value("${ai.deepseek.api-url}")
    private String apiUrl;

    @Value("${ai.deepseek.api-key}")
    private String apiKey;

    public String getApiUrl() {
        return apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);   // 连接超时 10s
        factory.setReadTimeout(60000);      // 读取超时 60s（AI 回复可能较慢）
        return new RestTemplate(factory);
    }
}
