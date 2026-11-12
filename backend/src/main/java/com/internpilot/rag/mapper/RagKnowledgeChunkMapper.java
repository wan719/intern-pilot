package com.internpilot.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.internpilot.rag.entity.RagKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagKnowledgeChunkMapper extends BaseMapper<RagKnowledgeChunk> {
}