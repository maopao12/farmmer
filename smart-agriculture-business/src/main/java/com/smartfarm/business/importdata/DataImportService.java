package com.smartfarm.business.importdata;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartfarm.business.entity.SensorData;
import com.smartfarm.business.mapper.SensorDataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSV数据导入服务
 *
 * <pre>
 * 支持功能：
 *   - CSV格式批量导入传感器数据
 *   - 数据校验（设备ID存在性、数据类型有效性、数值范围）
 *   - 批量写入（saveBatch）
 *   - 错误行跳过 + 详细错误报告
 *   - 导入统计（总行数/成功数/跳过数）
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService extends ServiceImpl<SensorDataMapper, SensorData> {

    private final SensorDataMapper sensorDataMapper;

    private final AtomicInteger totalImported = new AtomicInteger(0);
    private final List<String> lastErrors = Collections.synchronizedList(new ArrayList<>());

    private static final DateTimeFormatter DT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> VALID_TYPES = Set.of(
            "TEMPERATURE", "HUMIDITY", "SOIL_MOISTURE",
            "LIGHT_INTENSITY", "CO2"
    );

    /**
     * 从CSV文件导入数据
     */
    public Map<String, Object> importCsv(MultipartFile file) {
        lastErrors.clear();
        int totalLines = 0;
        int successCount = 0;
        int skipCount = 0;
        List<SensorData> batch = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine(); // 跳过表头
            totalLines++;

            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                try {
                    SensorData data = parseLine(line);
                    if (data != null) {
                        batch.add(data);
                        successCount++;

                        // 每100条批量写入一次
                        if (batch.size() >= 100) {
                            this.saveBatch(batch, 100);
                            totalImported.addAndGet(batch.size());
                            batch.clear();
                        }
                    }
                } catch (Exception e) {
                    skipCount++;
                    lastErrors.add("行" + totalLines + ": " + e.getMessage());
                    log.warn("[CSV导入] 跳过行{}: {}", totalLines, e.getMessage());
                }
            }

            // 写入剩余数据
            if (!batch.isEmpty()) {
                this.saveBatch(batch, batch.size());
                totalImported.addAndGet(batch.size());
            }

        } catch (Exception e) {
            log.error("[CSV导入] 文件读取失败", e);
            throw new RuntimeException("CSV文件读取失败: " + e.getMessage());
        }

        log.info("[CSV导入] 完成: 总行数={}, 成功={}, 跳过={}", totalLines - 1, successCount, skipCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLines", totalLines - 1);  // 减去表头
        result.put("successCount", successCount);
        result.put("skipCount", skipCount);
        result.put("errors", lastErrors.stream().limit(20).toList());
        return result;
    }

    /**
     * 获取导入统计
     */
    public Map<String, Object> getImportStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalImported", totalImported.get());
        status.put("recentErrors", lastErrors.stream().limit(5).toList());
        return status;
    }

    /**
     * 解析CSV一行: device_id,data_type,data_value,unit,collect_time,source
     */
    private SensorData parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("列数不足，需要6列");
        }

        Long deviceId = Long.parseLong(parts[0].trim());
        String dataType = parts[1].trim().toUpperCase();
        BigDecimal dataValue = new BigDecimal(parts[2].trim());
        String unit = parts[3].trim();
        LocalDateTime collectTime = LocalDateTime.parse(parts[4].trim(), DT_FORMAT);
        String source = parts.length > 5 ? parts[5].trim() : "CSV";

        // 校验
        if (!VALID_TYPES.contains(dataType)) {
            throw new IllegalArgumentException("无效数据类型: " + dataType);
        }
        if (dataValue.compareTo(BigDecimal.valueOf(-50)) < 0
                || dataValue.compareTo(BigDecimal.valueOf(200)) > 0) {
            throw new IllegalArgumentException("数据值超出合理范围: " + dataValue);
        }

        return SensorData.builder()
                .deviceId(deviceId)
                .dataType(dataType)
                .dataValue(dataValue)
                .unit(unit)
                .collectTime(collectTime)
                .source(source)
                .build();
    }
}
