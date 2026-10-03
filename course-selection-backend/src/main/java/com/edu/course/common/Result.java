package com.edu.course.common;

import lombok.Data;

/**
 * 统一返回结果类 — 贯穿整个系统的响应格式
 * 
 * 前端 request.js 响应拦截器依赖此结构:
 *   code=200 → 成功返回; code!=200 → Message.error(message)
 * 
 * @param <T> 泛型，可以是任意类型的数据(List<User>, Map, String等)
 */
@Data
public class Result<T> {

    private Integer code;     // 状态码: 200=成功, 500=业务异常(前端统一提示)
    private String message;   // 提示信息: 成功时为"success"，失败时为具体原因
    private T data;           // 响应数据: 可以是对象、列表、null

    private Result(Integer code, String message, T data) {  // 私有构造，强制使用静态工厂方法
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);  // 成功返回: code=200
    }

    public static <T> Result<T> success() {
        return success(null);  // 无数据的成功返回(如删除操作)
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);  // 默认错误: code=500
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);  // 自定义状态码的错误(如BusinessException携带特定code)
    }
}
