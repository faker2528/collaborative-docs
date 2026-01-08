package com.collab.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.Friendship;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友关系Mapper
 */
@Mapper
public interface FriendshipMapper extends BaseMapper<Friendship> {
}
