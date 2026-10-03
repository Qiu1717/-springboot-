package com.edu.course.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice  // 相当于@ControllerAdvice + @ResponseBody: 全局拦截Controller异常并返回JSON
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)  // 兜底: 捕获所有未被其他Handler处理的异常
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);  // 记录完整堆栈到日志
        return Result.error(e.getMessage());  // 向前端返回code=500的统一错误格式
    }

    @ExceptionHandler(BusinessException.class)  // 优先匹配: Spring会选择最具体的异常Handler
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());  // 业务异常用warn级别，不打印堆栈
        return Result.error(e.getCode(), e.getMessage());  // 返回自定义状态码(如重复选课返回500，前端展示具体原因)
    }
}
