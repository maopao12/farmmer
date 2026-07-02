package com.smartfarm.business.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库实体 — AI智能体RAG检索数据源
 *
 * @author SmartFarm Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标准问题 */
    private String question;

    /** 标准答案 */
    private String answer;

    /** 分类: IRRIGATION / FERTILIZER / PEST / DISEASE / GENERAL */
    private String category;

    /** 关键词(逗号分隔) */
    private String keywords;

    /** 关联作物 */
    private String cropType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
