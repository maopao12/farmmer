package com.smartfarm.business.alert;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfarm.business.entity.AlertLog;
import com.smartfarm.business.entity.AlertRule;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.business.mapper.AlertLogMapper;
import com.smartfarm.business.mapper.AlertRuleMapper;
import com.smartfarm.business.mapper.PlotMapper;
import com.smartfarm.common.R;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.RbacUtils;
import com.smartfarm.framework.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 告警管理控制器
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRuleMapper alertRuleMapper;
    private final AlertLogMapper alertLogMapper;
    private final PlotMapper plotMapper;

    /** 查询地块告警规则 */
    @GetMapping("/rule")
    public R<java.util.List<AlertRule>> rules(@RequestParam Long plotId) {
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) throw new BizException(404, "地块不存在");
        RbacUtils.checkOwnership(plot.getOwnerId(), "告警规则");
        return R.ok(alertRuleMapper.selectList(
                new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getPlotId, plotId)));
    }

    /** 创建/更新告警规则 */
    @PostMapping("/rule")
    public R<AlertRule> saveRule(@RequestBody AlertRule rule) {
        if (rule.getPlotId() != null) {
            Plot plot = plotMapper.selectById(rule.getPlotId());
            if (plot != null) RbacUtils.checkOwnership(plot.getOwnerId(), "告警规则");
        }
        if (rule.getId() != null) {
            alertRuleMapper.updateById(rule);
        } else {
            alertRuleMapper.insert(rule);
        }
        return R.ok(rule);
    }

    /**
     * 告警日志分页查询
     * FARMER 仅查自己地块的告警
     */
    @GetMapping("/log")
    public R<IPage<AlertLog>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String level) {
        Page<AlertLog> p = new Page<>(page, size);
        LambdaQueryWrapper<AlertLog> wrapper = new LambdaQueryWrapper<>();

        // FARMER: 只查自己地块的告警
        if (RbacUtils.isFarmer()) {
            // 先查用户的地块ID列表
            var plotIds = plotMapper.selectList(
                    new LambdaQueryWrapper<Plot>()
                            .eq(Plot::getOwnerId, UserContext.getCurrentUserId())
                            .select(Plot::getId)
            ).stream().map(Plot::getId).toList();
            if (plotIds.isEmpty()) {
                return R.ok(new Page<>());
            }
            wrapper.in(AlertLog::getPlotId, plotIds);
        }
        if (level != null) {
            wrapper.eq(AlertLog::getAlertLevel, level);
        }
        wrapper.orderByDesc(AlertLog::getTriggerTime);
        return R.ok(alertLogMapper.selectPage(p, wrapper));
    }

    /** 标记告警已读 */
    @PutMapping("/log/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        AlertLog log = new AlertLog();
        log.setId(id);
        log.setIsRead(1);
        alertLogMapper.updateById(log);
        return R.ok();
    }
}
