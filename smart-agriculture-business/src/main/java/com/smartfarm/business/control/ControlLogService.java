package com.smartfarm.business.control;

import com.smartfarm.business.entity.ControlLog;
import com.smartfarm.business.mapper.ControlLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 控制指令审计日志服务 — 独立事务，永不回滚
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ControlLogService {

    private final ControlLogMapper controlLogMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ControlLog insertSentLog(ControlLog entity) {
        entity.setCommandStatus("SENT");
        entity.setSendTime(LocalDateTime.now());
        controlLogMapper.insert(entity);
        log.info("[审计] 指令日志已持久化 id={}, deviceId={}, command={}, status=SENT",
                entity.getId(), entity.getDeviceId(), entity.getCommand());
        return entity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markSuccess(Long logId, String resultMsg, LocalDateTime responseTime) {
        ControlLog entity = new ControlLog();
        entity.setId(logId);
        entity.setCommandStatus("SUCCESS");
        entity.setResultMsg(resultMsg);
        entity.setResponseTime(responseTime);
        controlLogMapper.updateById(entity);
        log.info("[审计] 指令成功 id={}, responseTime={}", logId, responseTime);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markTimeout(Long logId, String reason) {
        ControlLog entity = new ControlLog();
        entity.setId(logId);
        entity.setCommandStatus("TIMEOUT");
        entity.setResultMsg(reason);
        controlLogMapper.updateById(entity);
        log.warn("[审计] 指令超时 id={}, reason={}", logId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long logId, String reason) {
        ControlLog entity = new ControlLog();
        entity.setId(logId);
        entity.setCommandStatus("FAILED");
        entity.setResultMsg(reason);
        controlLogMapper.updateById(entity);
        log.warn("[审计] 指令失败 id={}, reason={}", logId, reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ControlLog insertOfflineLog(ControlLog entity) {
        entity.setCommandStatus("FAILED");
        entity.setResultMsg("设备离线，指令下发失败");
        entity.setSendTime(LocalDateTime.now());
        controlLogMapper.insert(entity);
        log.warn("[审计] 离线拦截 id={}, deviceId={}", entity.getId(), entity.getDeviceId());
        return entity;
    }
}
