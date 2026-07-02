package com.smartfarm.business.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.*;
import com.smartfarm.business.mapper.KnowledgeBaseMapper;
import com.smartfarm.business.mapper.PlotMapper;
import com.smartfarm.business.mapper.SensorDataMapper;
import com.smartfarm.common.exception.BizException;
import com.smartfarm.framework.security.RbacUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索增强生成服务
 *
 * <pre>
 * 处理流程:
 *   ① 校验地块归属
 *   ② 查询该地块所有SENSOR设备最新 sensor_data → 组装环境上下文
 *   ③ 关键词匹配 knowledge_base → 获取参考知识
 *   ④ 组装 Prompt: [环境参数] + [知识库参考] + 用户问题
 *   ⑤ 规则引擎匹配 → 未匹配则降级LLM（或直接返回规则结果）
 *
 * 当前默认使用规则引擎模式（校内演示可用），
 * 配置 ai.mode=llm 切换为大模型模式。
 * </pre>
 *
 * @author SmartFarm Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final SensorDataMapper sensorDataMapper;
    private final PlotMapper plotMapper;

    /**
     * AI问答入口
     */
    public Map<String, Object> chat(String question, Long plotId) {
        // ① 校验归属
        Plot plot = plotMapper.selectById(plotId);
        if (plot == null) throw new BizException(404, "地块不存在");
        RbacUtils.checkOwnership(plot.getOwnerId(), "AI问答");

        // ② 组装环境上下文
        String envContext = buildEnvironmentContext(plotId);

        // ③ RAG检索知识库
        List<KnowledgeBase> matchedKnowledge = searchKnowledge(question);

        // ④ 规则匹配
        String answer = matchRule(question, envContext, matchedKnowledge);

        // ⑤ 构建响应
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("plotName", plot.getName());
        result.put("environmentContext", envContext);
        result.put("references", matchedKnowledge.stream()
                .map(k -> Map.of("question", k.getQuestion(), "category", k.getCategory()))
                .limit(3).collect(Collectors.toList()));
        return result;
    }

    /**
     * 组装地块实时环境参数上下文
     */
    private String buildEnvironmentContext(Long plotId) {
        // 查询地块下所有SENSOR的最新数据
        List<SensorData> latestDataList = sensorDataMapper.selectList(
                new LambdaQueryWrapper<SensorData>()
                        .inSql(SensorData::getDeviceId,
                                "SELECT id FROM device WHERE plot_id = " + plotId
                                        + " AND device_category = 'SENSOR'")
                        .orderByDesc(SensorData::getCollectTime)
                        .last("LIMIT 10")
        );

        if (latestDataList.isEmpty()) {
            return "暂无实时传感器数据";
        }

        // 聚合: 每种数据类型取最新的值
        Map<String, SensorData> latestByType = new LinkedHashMap<>();
        for (SensorData d : latestDataList) {
            latestByType.putIfAbsent(d.getDataType(), d);
        }

        StringBuilder sb = new StringBuilder("当前地块实时环境参数: ");
        for (SensorData d : latestByType.values()) {
            sb.append(String.format("%s=%.1f%s, ",
                    d.getDataType(), d.getDataValue(), d.getUnit()));
        }
        // 去除末尾逗号
        String context = sb.toString();
        if (context.endsWith(", ")) {
            context = context.substring(0, context.length() - 2) + "。";
        }
        return context;
    }

    /**
     * 知识库关键词检索
     */
    private List<KnowledgeBase> searchKnowledge(String question) {
        // 简单分词：按空格和常见标点拆
        String[] words = question.split("[\\s，。？?！!]+");
        List<KnowledgeBase> results = new ArrayList<>();

        for (String word : words) {
            if (word.length() < 2) continue; // 跳过多余的单字
            List<KnowledgeBase> matched = knowledgeBaseMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeBase>()
                            .like(KnowledgeBase::getKeywords, word)
                            .or()
                            .like(KnowledgeBase::getQuestion, word)
            );
            results.addAll(matched);
        }

        // 去重并限制数量
        return results.stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 规则引擎匹配
     * <p>
     * 基于知识库检索结果 + 环境参数阈值判断，生成针对性回答。
     * 若无法匹配，返回通用回答。
     */
    private String matchRule(String question, String envContext,
                              List<KnowledgeBase> knowledgeList) {
        if (!knowledgeList.isEmpty()) {
            // 用最匹配的知识库答案
            KnowledgeBase best = knowledgeList.get(0);
            return best.getAnswer() + "\n\n（参考当前环境: " + envContext + "）";
        }

        // 阈值规则判断
        String q = question.toLowerCase();
        if (q.contains("浇水") || q.contains("灌溉") || q.contains("湿度")) {
            if (envContext.contains("湿度")) {
                return "建议您查看当前土壤湿度数值。若湿度低于45%，建议开启灌溉系统进行滴灌。若湿度在60%-80%之间，暂不需要灌溉。\n\n（参考当前环境: " + envContext + "）";
            }
            return "请查看实时传感器数据中的土壤湿度数值，根据作物需求决定是否灌溉。";
        }
        if (q.contains("温度") || q.contains("高温") || q.contains("通风")) {
            return "建议关注温度变化趋势。若温度超过35°C，应及时采取通风降温措施。\n\n（参考当前环境: " + envContext + "）";
        }

        // 通用兜底
        return "您的问题已收到。当前环境参数为: " + envContext
                + "。建议结合具体作物生长阶段和气象条件进行判断。如需更详细的农事指导，请提供更多信息。";
    }
}
