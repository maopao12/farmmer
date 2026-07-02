package com.smartfarm.business.plot;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smartfarm.business.entity.Plot;
import com.smartfarm.common.R;
import com.smartfarm.framework.security.RbacUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地块管理控制器
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/plot")
@RequiredArgsConstructor
public class PlotController {

    private final PlotService plotService;

    /** 分页查询地块列表（ADMIN查全部，FARMER仅自己的） */
    @GetMapping("/list")
    public R<IPage<Plot>> list(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        return R.ok(plotService.listPlots(page, size));
    }

    /** 全部地块（下拉选择器用） */
    @GetMapping("/all")
    public R<List<Plot>> all() {
        return R.ok(plotService.listAllPlots());
    }

    /** 地块综合概览（含设备列表+最新数据） */
    @GetMapping("/{id}/overview")
    public R<PlotOverview> overview(@PathVariable Long id) {
        return R.ok(plotService.getPlotOverview(id));
    }

    /** 新增地块 — ADMIN ONLY */
    @PostMapping
    public R<Plot> create(@RequestBody Plot plot) {
        RbacUtils.requireAdmin();
        return R.ok(plotService.createPlot(plot));
    }

    /** 修改地块 — ADMIN ONLY */
    @PutMapping("/{id}")
    public R<Plot> update(@PathVariable Long id, @RequestBody Plot plot) {
        RbacUtils.requireAdmin();
        return R.ok(plotService.updatePlot(id, plot));
    }

    /** 删除地块 — ADMIN ONLY（需先解绑设备） */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        RbacUtils.requireAdmin();
        plotService.deletePlot(id);
        return R.ok();
    }
}
