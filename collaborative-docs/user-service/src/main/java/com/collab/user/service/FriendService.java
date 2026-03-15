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
    List<UserDTO> getFriendList(String userId);
    
    /**
     * 发送好友请求
     */
    void sendFriendRequest(String fromUserId, SendFriendRequest request);
    
    /**
     * 获取收到的好友请求列表
     */
    List<FriendRequestDTO> getReceivedRequests(String userId);
    
    /**
     * 获取发送的好友请求列表
     */
    List<FriendRequestDTO> getSentRequests(String userId);
    
    /**
     * 获取待处理的好友请求数量
     */
    int getPendingRequestCount(String userId);
    
    /**
     * 同意好友请求
     */
    void acceptRequest(String requestId, String userId);
    
    /**
     * 拒绝好友请求
     */
    void rejectRequest(String requestId, String userId);
    
    /**
     * 删除好友
     */
    void deleteFriend(String userId, String friendId);
    
    /**
     * 检查是否为好友
     */
    boolean isFriend(String userId, String friendId);
}
