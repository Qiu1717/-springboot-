package com.edu.course.common;

import lombok.Getter;

/**
 * 自定义业务异常类
 * 在Service层抛出 → GlobalExceptionHandler捕获 → 转为Result.error返回前端
 * 
 * 使用示例:
 *   throw new BusinessException("课程已满员");           // code默认500
 *   throw new BusinessException(400, "参数不合法");      // 自定义code
 */
@Getter
public class BusinessException extends RuntimeException {  // 继承RuntimeException，不强制try-catch

    private Integer code;  // 异常状态码

    public BusinessException(String message) {
        super(message);
        this.code = 500;  // 默认500，前端response拦截器会显示message内容
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
