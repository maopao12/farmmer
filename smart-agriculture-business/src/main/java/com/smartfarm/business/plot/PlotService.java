package com.smartfarm.business.plot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfarm.business.entity.Device;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.business.entity.SensorData;
import com.smartfarm.business.mapper.DeviceMapper;
import com.smartfarm.business.mapper.PlotMapper;
import com.smartfarm.business.mapper.SensorDataMapper;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 地块管理服务 — 含RBAC动态数据权限过滤
 *
 * <pre>
 * 权限规则：
 *   ADMIN: 跳过 owner_id 过滤，可查看/管理所有地块
 *   FARMER: 强制 SQL 附加 WHERE owner_id = currentUserId，横向越权拦截
 *
 * 这是生产级设计 —— 不能一刀切过滤，否则ADMIN无法工作。
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotMapper plotMapper;
    private final DeviceMapper deviceMapper;
    private final SensorDataMapper sensorDataMapper;

    // ==================== 查询：RBAC动态过滤 ====================

    /**
     * 分页查询地块列表。
     * <p>
     * ADMIN → 返回全部地块
     * FARMER → 仅返回自己归属的地块
     */
    public IPage<Plot> listPlots(int pageNum, int pageSize) {
        Page<Plot> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Plot> wrapper = new LambdaQueryWrapper<>();

        // RBAC: ADMIN跳过过滤，FARMER附加owner_id条件
        applyOwnerFilter(wrapper);

        wrapper.orderByDesc(Plot::getCreateTime);
        return plotMapper.selectPage(page, wrapper);
    }

    /**
     * 查询全部地块（不分页），用于下拉选择器等场景
     */
    public List<Plot> listAllPlots() {
        LambdaQueryWrapper<Plot> wrapper = new LambdaQueryWrapper<>();
        applyOwnerFilter(wrapper);
        wrapper.orderByDesc(Plot::getCreateTime);
        return plotMapper.selectList(wrapper);
    }

    /**
     * 地块综合概览 — 传感器数据 + 控制设备列表 + 在线状态
     * <p>
     * 返回内容：
     *   - 地块基本信息
     *   - 所有设备列表（含 sensor 和 controller）
     *   - 每个 SENSOR 设备的最新一条数据
     *   - CONTROLLER 设备列表及在线状态（用于前端空状态判断）
     */
    public PlotOverview getPlotOverview(Long plotId) {
        // 查询地块 → RBAC校验
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) {
            throw new BizException(404, "地块不存在");
        }
        // RBAC: ADMIN跳过，FARMER校验归属
        if (!UserContext.isAdmin() && !plot.getOwnerId().equals(UserContext.getCurrentUserId())) {
            log.warn("[越权拦截] 用户{} 尝试查看不属于自己的地块{}",
                    UserContext.getCurrentUserId(), plotId);
            throw BizException.noPermission();
        }

        // 查询该地块下所有设备
        List<Device> allDevices = deviceMapper.selectList(
                new LambdaQueryWrapper<Device>().eq(Device::getPlotId, plotId)
        );

        // 按 category 分组
        List<Device> sensors = allDevices.stream()
                .filter(d -> "SENSOR".equals(d.getDeviceCategory()))
                .collect(Collectors.toList());
        List<Device> controllers = allDevices.stream()
                .filter(d -> "CONTROLLER".equals(d.getDeviceCategory()))
                .collect(Collectors.toList());

        // 查询每个SENSOR的最新数据
        Map<Long, SensorData> latestDataMap = sensors.stream()
                .collect(Collectors.toMap(
                        Device::getId,
                        s -> sensorDataMapper.selectOne(
                                new LambdaQueryWrapper<SensorData>()
                                        .eq(SensorData::getDeviceId, s.getId())
                                        .orderByDesc(SensorData::getCollectTime)
                                        .last("LIMIT 1")
                        )
                ));

        // 组装返回
        return PlotOverview.builder()
                .plot(plot)
                .sensors(sensors)
                .controllers(controllers)
                .latestSensorData(latestDataMap)
                .build();
    }

    // ==================== 增删改：ADMIN ONLY ====================

    public Plot createPlot(Plot plot) {
        if (!UserContext.isAdmin()) {
            throw BizException.noPermission();
        }
        plotMapper.insert(plot);
        log.info("[地块创建] ADMIN{} 创建地块: {}", UserContext.getCurrentUserId(), plot.getName());
        return plot;
    }

    public Plot updatePlot(Long plotId, Plot update) {
        if (!UserContext.isAdmin()) {
            throw BizException.noPermission();
        }
        Plot existing = plotMapper.selectById(plotId);
        if (existing == null) {
            throw new BizException(404, "地块不存在");
        }
        update.setId(plotId);
        plotMapper.updateById(update);
        log.info("[地块更新] ADMIN{} 更新地块: id={}", UserContext.getCurrentUserId(), plotId);
        return plotMapper.selectById(plotId);
    }

    public void deletePlot(Long plotId) {
        if (!UserContext.isAdmin()) {
            throw BizException.noPermission();
        }
        // 检查是否还有绑定的设备
        Long deviceCount = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>().eq(Device::getPlotId, plotId)
        );
        if (deviceCount > 0) {
            Plot plot = plotMapper.selectById(plotId);
            throw BizException.plotHasDevices(
                    plot != null ? plot.getName() : "未知地块",
                    deviceCount.intValue()
            );
        }
        plotMapper.deleteById(plotId);
        log.info("[地块删除] ADMIN{} 删除地块: id={}", UserContext.getCurrentUserId(), plotId);
    }

    // ==================== 私有：RBAC过滤逻辑 ====================

    /**
     * RBAC动态权限过滤核心方法。
     * <p>
     * ADMIN → 不过滤，返回空 wrapper（查全部）
     * FARMER → 附加 WHERE owner_id = currentUserId
     * <p>
     * 这就是分析指出的修正：不能一刀切 WHERE owner_id，
     * 必须先判断角色，ADMIN需要全局视图。
     */
    private void applyOwnerFilter(LambdaQueryWrapper<Plot> wrapper) {
        if (!UserContext.isAdmin()) {
            Long currentUserId = UserContext.getCurrentUserId();
            wrapper.eq(Plot::getOwnerId, currentUserId);
            log.debug("[RBAC] FARMER{} 附加owner过滤", currentUserId);
        } else {
            log.debug("[RBAC] ADMIN{} 跳过owner过滤，返回全局数据", UserContext.getCurrentUserId());
        }
    }
}
