package com.smartfarm.simulator;

/**
 * 数据采集器接口 — 模拟/真实硬件切换的核心抽象
 * <p>
 * 校内模式: MockDataCollector (定时动态轮询)
 * 基地模式: MqttDataCollector (订阅MQTT Topic)
 * <p>
 * 切换方式: application.yml → data.collector: mock | mqtt
 *
 * @author SmartFarm Team
 */
public interface DataCollector {

    /** 启动采集器 */
    void start();

    /** 停止采集器 */
    void stop();

    /** 采集器名称 */
    String getName();
}
