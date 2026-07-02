package com.smartfarm.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应封装
 * <p>
 * 所有API接口统一返回格式: { code: 200, message: "success", data: {...} }
 *
 * @author SmartFarm Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {

    /** 状态码: 200成功, 400参数错误, 403无权限, 500服务端错误 */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    // ==================== 静态工厂方法 ====================

    public static <T> R<T> ok() {
        return new R<>(200, "success", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(200, message, data);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(String message) {
        return new R<>(400, message, null);
    }

    public static <T> R<T> error(String message) {
        return new R<>(500, message, null);
    }

    // ==================== 常用快捷方法 ====================

    public static <T> R<T> forbidden(String message) {
        return new R<>(403, message, null);
    }

    public static <T> R<T> badRequest(String message) {
        return new R<>(400, message, null);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
