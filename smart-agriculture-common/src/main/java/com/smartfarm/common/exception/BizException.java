package com.smartfarm.common.exception;

import lombok.Getter;

/**
 * 业务异常 — 所有业务校验失败统一抛出此异常
 * <p>
 * GlobalExceptionHandler 捕获后转换为 R.fail(code, message) 返回前端
 *
 * @author SmartFarm Team
 */
@Getter
public class BizException extends RuntimeException {

    /** HTTP状态码: 400/403/500 */
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }

    // ==================== 预定义异常工厂 ====================

    /** 设备离线 */
    public static BizException deviceOffline(String deviceName) {
        return new BizException(400, "设备 [" + deviceName + "] 当前离线，指令下发失败");
    }

    /** 传感器不支持控制 */
    public static BizException sensorNotControllable(String deviceName) {
        return new BizException(400, "设备 [" + deviceName + "] 为传感器类型，不支持控制操作");
    }

    /** 无权操作 */
    public static BizException noPermission() {
        return new BizException(403, "无权操作此设备，请联系管理员");
    }

    /** 设备已被绑定 */
    public static BizException deviceAlreadyBound(String deviceCode, String plotName) {
        return new BizException(400,
                "设备 [" + deviceCode + "] 已绑定到 [" + plotName + "]，请先解绑后再操作");
    }

    /** 指令超时 */
    public static BizException commandTimeout(String deviceName) {
        return new BizException(400,
                "设备 [" + deviceName + "] 连接超时，10秒内未收到响应");
    }

    /** 地块下有设备未解绑 */
    public static BizException plotHasDevices(String plotName, int deviceCount) {
        return new BizException(400,
                "地块 [" + plotName + "] 下还有 " + deviceCount + " 台设备未解绑，请先解绑所有设备");
    }
}
