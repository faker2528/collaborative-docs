package com.collab.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}
