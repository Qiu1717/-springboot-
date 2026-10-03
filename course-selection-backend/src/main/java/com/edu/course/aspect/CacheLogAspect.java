package com.edu.course.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 缓存日志切面 — 打印 命中🟢 / 未命中🔵 / 驱逐🟡 三种事件
 * @Order(0) 确保在 Spring Cache 拦截器之前执行
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class CacheLogAspect {

    @Autowired
    private CacheManager cacheManager;

    /** 拦截标注了 @Cacheable 的 Service 方法 */
    @Around("execution(* com.edu.course.service.*.*(..)) && @annotation(cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
        String cacheName = cacheable.value().length > 0 ? cacheable.value()[0] : "unknown";
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return pjp.proceed();

        // 生成缓存 key
        Object key = buildKey(pjp, cacheable);

        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            log.info("🟢 [Redis] 缓存命中 {}::{}，直接从 Redis 返回（未查 MySQL）", cacheName, key);
            return cached.get();
        }
        log.info("🔵 [Redis] 缓存未命中 {}::{}，查询 MySQL …", cacheName, key);
        return pjp.proceed();
    }

    /** 拦截标注了 @CacheEvict 的 Service 方法 — 打印驱逐日志 */
    @Around("execution(* com.edu.course.service.*.*(..)) && @annotation(evict)")
    public Object aroundCacheEvict(ProceedingJoinPoint pjp, CacheEvict evict) throws Throwable {
        String[] cacheNames = evict.value().length > 0 ? evict.value() : new String[]{"unknown"};
        boolean allEntries = evict.allEntries();
        String key = evict.key().isEmpty() ? null : evict.key();

        Object result = pjp.proceed(); // 先执行业务方法，让 Spring Cache 完成驱逐

        for (String name : cacheNames) {
            if (allEntries) {
                log.info("🟡 [Redis] 清除缓存 {}（allEntries=true）", name);
            } else if (key != null && !key.isEmpty()) {
                log.info("🟡 [Redis] 清除缓存 {}::{}", name, key);
            } else {
                log.info("🟡 [Redis] 清除缓存 {}", name);
            }
        }
        return result;
    }

    /** 缓存 key 生成（与 Spring Cache 默认策略对齐） */
    private Object buildKey(ProceedingJoinPoint pjp, Cacheable cacheable) {
        if (cacheable.key() != null && !cacheable.key().isEmpty()) {
            return pjp.getArgs().length > 0 ? pjp.getArgs()[0] : SimpleKey.EMPTY;
        }
        return pjp.getArgs().length == 0 ? SimpleKey.EMPTY : new SimpleKey(pjp.getArgs());
    }
}
