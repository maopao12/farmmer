package com.smartfarm.business.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfarm.business.entity.KnowledgeBase;
import com.smartfarm.business.mapper.KnowledgeBaseMapper;
import com.smartfarm.common.R;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI智能体控制器 — 农事问答 + 知识库管理
 *
 * @author SmartFarm Team
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * AI农事问答
     * 请求体: { "question": "当前土壤湿度合适吗？", "plotId": 1 }
     * 后端自动注入该地块的实时环境参数作为上下文
     */
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        return R.ok(ragService.chat(request.getQuestion(), request.getPlotId()));
    }

    /** 知识库列表（按分类筛选） */
    @GetMapping("/knowledge")
    public R<List<KnowledgeBase>> knowledge(
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (category != null) {
            wrapper.eq(KnowledgeBase::getCategory, category);
        }
        return R.ok(knowledgeBaseMapper.selectList(wrapper));
    }

    @Data
    public static class ChatRequest {
        private String question;
        private Long plotId;
    }
}
