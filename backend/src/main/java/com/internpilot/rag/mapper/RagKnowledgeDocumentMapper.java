package com.internpilot.rag.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.rag.entity.RagKnowledgeDocument;

@Mapper
public interface RagKnowledgeDocumentMapper  extends BaseMapper<RagKnowledgeDocument> {
    
}
