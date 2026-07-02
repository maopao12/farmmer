package com.smartfarm.business.control;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfarm.business.entity.ControlLog;
import com.smartfarm.business.mapper.ControlLogMapper;
import com.smartfarm.common.R;
import com.smartfarm.framework.security.RbacUtils;
import com.smartfarm.framework.security.UserContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设备控制控制器
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/control")
@RequiredArgsConstructor
public class ControlController {

    private final ControlService controlService;
    private final ControlLogMapper controlLogMapper;

    /**
     * 下发灌溉控制指令
     * 请求体: { "deviceId": 2, "command": "ON", "duration": 30 }
     */
    @PostMapping("/irrigation")
    public R<ControlLog> irrigation(@RequestBody IrrigationRequest request) {
        ControlLog result = controlService.executeCommand(
                request.getDeviceId(), request.getCommand(), request.getDuration());
        return R.ok(result);
    }

    /** 控制日志分页查询（仅查当前用户操作的） */
    @GetMapping("/log")
    public R<IPage<ControlLog>> log(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long deviceId) {
        Page<ControlLog> p = new Page<>(page, size);
        LambdaQueryWrapper<ControlLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ControlLog::getOperatorId, UserContext.getCurrentUserId());
        if (deviceId != null) {
            wrapper.eq(ControlLog::getDeviceId, deviceId);
        }
        wrapper.orderByDesc(ControlLog::getSendTime);
        return R.ok(controlLogMapper.selectPage(p, wrapper));
    }

    @Data
    public static class IrrigationRequest {
        private Long deviceId;
        private String command;
        private Integer duration;
    }
}
