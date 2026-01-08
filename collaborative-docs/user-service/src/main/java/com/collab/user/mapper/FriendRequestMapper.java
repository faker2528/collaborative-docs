package com.collab.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.common.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;

/**
 * 好友请求Mapper
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {
}
