package com.smartfarm.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度配置 — 多线程池隔离
 *
 * <pre>
 * 问题：Spring Boot 默认 @Scheduled 使用单线程执行。
 * 当 MockDataCollector (每5秒) 和 HeartbeatChecker (每60秒)
 * 共享同一个线程时，MockDataCollector 的阻塞会导致
 * HeartbeatChecker 失效，设备离线检测永远不触发。
 *
 * 修复：配置 pool-size = 10 的 ThreadPoolTaskScheduler，
 * 确保数据采集、心跳检测、告警扫描各自独立线程运行。
 * </pre>
 *
 * @author SmartFarm Team
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    /**
     * 自定义 TaskScheduler — pool-size 至少 10
     * <p>
     * 线程分配预估：
     *   - MockDataCollector: 1-2个 (核心采集)
     *   - HeartbeatChecker:   1个   (心跳检测)
     *   - 告警引擎内部扫描:    1-2个 (由Collector触发)
     *   - 预留:              5个   (未来扩展MQTT重连等)
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("smartfarm-scheduled-");
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        return scheduler;
    }
}
