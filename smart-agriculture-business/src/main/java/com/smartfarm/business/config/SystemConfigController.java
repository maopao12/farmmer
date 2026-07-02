package com.smartfarm.business.config;

import com.smartfarm.common.R;
import com.smartfarm.framework.security.RbacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统配置控制器 — 暴露可调参数给管理员 Settings 页面
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemConfigController {

    @Value("${smartfarm.data.interval:5}")
    private int dataInterval;

    @Value("${smartfarm.heartbeat.timeout:3}")
    private int heartbeatTimeout;

    @Value("${smartfarm.command.timeout:10}")
    private int commandTimeout;

    /**
     * 读取当前系统配置
     */
    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        RbacUtils.requireAdmin();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("dataInterval", dataInterval);
        config.put("heartbeatTimeout", heartbeatTimeout);
        config.put("commandTimeout", commandTimeout);
        return R.ok(config);
    }

    /**
     * 系统健康检查
     */
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("timestamp", System.currentTimeMillis());
        return R.ok(info);
    }
}
