package com.smartfarm.business.device;

import com.smartfarm.business.entity.Device;
import com.smartfarm.common.R;
import com.smartfarm.framework.security.RbacUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备管理控制器
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/list")
    public R<List<Device>> list(@RequestParam Long plotId) {
        return R.ok(deviceService.listByPlot(plotId));
    }

    /** 仓库中未绑定设备 — ADMIN ONLY */
    @GetMapping("/unbound")
    public R<List<Device>> unbound() {
        RbacUtils.requireAdmin();
        return R.ok(deviceService.listUnbound());
    }

    /** 绑定设备到地块 — ADMIN ONLY */
    @PostMapping("/bind")
    public R<Void> bind(@RequestBody Map<String, Long> body) {
        RbacUtils.requireAdmin();
        deviceService.bindDevice(body.get("deviceId"), body.get("plotId"));
        return R.ok();
    }

    /** 解绑设备 — ADMIN ONLY */
    @PostMapping("/unbind/{id}")
    public R<Void> unbind(@PathVariable Long id) {
        RbacUtils.requireAdmin();
        deviceService.unbindDevice(id);
        return R.ok();
    }

    @GetMapping("/status/{id}")
    public R<Device> status(@PathVariable Long id) {
        return R.ok(deviceService.getStatus(id));
    }
}
