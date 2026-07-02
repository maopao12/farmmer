package com.smartfarm.business.device;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.business.mapper.PlotMapper;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.RbacUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备管理服务 — 绑定/解绑 + 仓库管理
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final PlotMapper plotMapper;

    /** 查询地块下的设备列表（RBAC校验地块归属） */
    public List<Device> listByPlot(Long plotId) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) throw new BizException(404, "地块不存在");
        RbacUtils.checkOwnership(plot.getOwnerId(), "地块设备");
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getPlotId, plotId));
    }

    /** 查询仓库中未绑定设备 — ADMIN ONLY */
    public List<Device> listUnbound() {
        RbacUtils.requireAdmin();
        return deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().isNull(Device::getPlotId));
    }

    /** 绑定设备到地块 — ADMIN ONLY */
    public void bindDevice(Long deviceId, Long plotId) {
        RbacUtils.requireAdmin();
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) throw new BizException(404, "设备不存在");
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) throw new BizException(404, "地块不存在");

        // 校验设备未被占用
        if (device.getPlotId() != null && !device.getPlotId().equals(plotId)) {
            Plot boundPlot = plotMapper.selectById(device.getPlotId());
            throw BizException.deviceAlreadyBound(device.getDeviceCode(),
                    boundPlot != null ? boundPlot.getName() : "未知地块");
        }

        device.setPlotId(plotId);
        deviceMapper.updateById(device);
        log.info("[设备绑定] 设备{}({}) 绑定到地块{}({}), 操作人=ADMIN{}",
                device.getDeviceCode(), device.getDeviceName(),
                plot.getName(), plotId, RbacUtils.getCurrentUserId());
    }

    /** 解绑设备 — ADMIN ONLY */
    public void unbindDevice(Long deviceId) {
        RbacUtils.requireAdmin();
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) throw new BizException(404, "设备不存在");

        log.info("[设备解绑] 设备{}({}) 从地块{}解绑",
                device.getDeviceCode(), device.getDeviceName(), device.getPlotId());

        device.setPlotId(null);
        device.setStatus("OFFLINE");
        deviceMapper.updateById(device);
    }

    /** 设备在线状态 */
    public Device getStatus(Long deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) throw new BizException(404, "设备不存在");
        // RBAC: 校验设备归属地块的owner
        if (device.getPlotId() != null) {
            Plot plot = plotMapper.selectById(device.getPlotId());
            if (plot != null) {
                RbacUtils.checkOwnership(plot.getOwnerId(), "设备状态");
            }
        }
        return device;
    }
}
