package com.collab.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.DocumentPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 文档权限Mapper
 */
@Mapper
public interface DocumentPermissionMapper extends BaseMapper<DocumentPermission> {
    
    /**
     * 获取用户对文档的权限类型
     */
    @Select("SELECT permission_type FROM t_document_permission WHERE document_id = #{documentId} AND user_id = #{userId}")
    Integer getPermissionType(@Param("documentId") Long documentId, @Param("userId") Long userId);
}
