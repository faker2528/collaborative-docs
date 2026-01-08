package com.collab.user.service;

import com.collab.common.dto.FriendRequestDTO;
import com.collab.common.dto.SendFriendRequest;
import com.collab.common.dto.UserDTO;

import java.util.List;

/**
 * 好友服务接口
 */
public interface FriendService {
    
    /**
     * 获取好友列表
     */
    List<UserDTO> getFriendList(Long userId);
    
    /**
     * 发送好友请求
     */
    void sendFriendRequest(Long fromUserId, SendFriendRequest request);
    
    /**
     * 获取收到的好友请求列表
     */
    List<FriendRequestDTO> getReceivedRequests(Long userId);
    
    /**
     * 获取发送的好友请求列表
     */
    List<FriendRequestDTO> getSentRequests(Long userId);
    
    /**
     * 获取待处理的好友请求数量
     */
    int getPendingRequestCount(Long userId);
    
    /**
     * 同意好友请求
     */
    void acceptRequest(Long requestId, Long userId);
    
    /**
     * 拒绝好友请求
     */
    void rejectRequest(Long requestId, Long userId);
    
    /**
     * 删除好友
     */
    void deleteFriend(Long userId, Long friendId);
    
    /**
     * 检查是否为好友
     */
    boolean isFriend(Long userId, Long friendId);
}
