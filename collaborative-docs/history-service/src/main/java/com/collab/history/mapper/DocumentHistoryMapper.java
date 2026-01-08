package com.collab.history.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.DocumentHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档历史Mapper
 */
@Mapper
public interface DocumentHistoryMapper extends BaseMapper<DocumentHistory> {
}
