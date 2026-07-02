package com.smartfarm.business.importdata;

import com.smartfarm.business.entity.SensorData;
import com.smartfarm.common.R;
import com.smartfarm.framework.security.RbacUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 数据导入控制器 — 支持CSV批量导入传感器数据
 *
 * <pre>
 * CSV格式要求:
 *   device_id,data_type,data_value,unit,collect_time,source
 *
 * 示例:
 *   1,TEMPERATURE,26.5,°C,2026-07-01 14:00:00,MOCK
 *   1,HUMIDITY,68.0,%,2026-07-01 14:00:00,MOCK
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    /**
     * CSV文件导入传感器数据
     * ADMIN ONLY
     */
    @PostMapping("/csv")
    public R<Map<String, Object>> importCsv(@RequestParam("file") MultipartFile file) {
        RbacUtils.requireAdmin();
        Map<String, Object> result = dataImportService.importCsv(file);
        return R.ok(result);
    }

    /**
     * 获取导入状态
     */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        return R.ok(dataImportService.getImportStatus());
    }

    /**
     * 下载CSV导入模板
     */
    @GetMapping("/template")
    public R<String> template() {
        return R.ok("""
                device_id,data_type,data_value,unit,collect_time,source
                1,TEMPERATURE,26.5,°C,2026-07-01 14:00:00,MOCK
                1,HUMIDITY,68.0,%,2026-07-01 14:00:00,MOCK
                1,LIGHT_INTENSITY,850,lux,2026-07-01 14:00:00,MOCK
                3,TEMPERATURE,31.2,°C,2026-07-01 14:00:00,MOCK
                3,HUMIDITY,55.0,%,2026-07-01 14:00:00,MOCK""");
    }
}
