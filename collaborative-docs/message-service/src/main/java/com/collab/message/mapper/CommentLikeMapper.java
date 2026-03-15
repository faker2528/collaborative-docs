package com.collab.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞 Mapper
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
    // 使用 MyBatis Plus 提供的标准方法
}
