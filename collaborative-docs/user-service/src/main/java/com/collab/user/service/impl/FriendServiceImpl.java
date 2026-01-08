package com.collab.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.collab.common.dto.FriendRequestDTO;
import com.collab.common.dto.SendFriendRequest;
import com.collab.common.dto.UserDTO;
import com.collab.common.entity.FriendRequest;
import com.collab.common.entity.Friendship;
import com.collab.common.entity.User;
import com.collab.common.exception.BusinessException;
import com.collab.user.mapper.FriendRequestMapper;
import com.collab.user.mapper.FriendshipMapper;
import com.collab.user.mapper.UserMapper;
import com.collab.user.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 好友服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements FriendService {
    
    private final FriendshipMapper friendshipMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final UserMapper userMapper;
    
    @Override
    public List<UserDTO> getFriendList(Long userId) {
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getUserId, userId)
               .eq(Friendship::getStatus, 1);
        
        List<Friendship> friendships = friendshipMapper.selectList(wrapper);
        if (friendships.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> friendIds = friendships.stream()
                .map(Friendship::getFriendId)
                .collect(Collectors.toList());
        
        List<User> friends = userMapper.selectBatchIds(friendIds);
        return friends.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendFriendRequest(Long fromUserId, SendFriendRequest request) {
        Long toUserId = Long.parseLong(request.getToUserId());
        
        // 不能添加自己为好友
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException("不能添加自己为好友");
        }
        
        // 检查是否已经是好友
        if (isFriend(fromUserId, toUserId)) {
            throw new BusinessException("对方已经是你的好友");
        }
        
        // 检查是否有待处理的请求
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, fromUserId)
               .eq(FriendRequest::getToUserId, toUserId)
               .eq(FriendRequest::getStatus, 0);
        if (friendRequestMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已发送过好友请求，请等待对方处理");
        }
        
        // 检查对方是否已经向我发送请求
        LambdaQueryWrapper<FriendRequest> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(FriendRequest::getFromUserId, toUserId)
                      .eq(FriendRequest::getToUserId, fromUserId)
                      .eq(FriendRequest::getStatus, 0);
        FriendRequest existingRequest = friendRequestMapper.selectOne(reverseWrapper);
        if (existingRequest != null) {
            // 对方已经向我发送请求，直接同意
            acceptRequest(existingRequest.getId(), fromUserId);
            return;
        }
        
        // 创建好友请求
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(toUserId);
        friendRequest.setMessage(request.getMessage());
        friendRequest.setStatus(0);
        friendRequestMapper.insert(friendRequest);
        
        log.info("User {} sent friend request to user {}", fromUserId, toUserId);
    }
    
    @Override
    public List<FriendRequestDTO> getReceivedRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
               .orderByDesc(FriendRequest::getCreateTime);
        
        List<FriendRequest> requests = friendRequestMapper.selectList(wrapper);
        return requests.stream().map(req -> {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(String.valueOf(req.getId()));
            dto.setFromUserId(String.valueOf(req.getFromUserId()));
            dto.setToUserId(String.valueOf(req.getToUserId()));
            dto.setMessage(req.getMessage());
            dto.setStatus(req.getStatus());
            dto.setCreateTime(req.getCreateTime());
            
            // 获取发送者信息
            User fromUser = userMapper.selectById(req.getFromUserId());
            if (fromUser != null) {
                dto.setFromUsername(fromUser.getUsername());
                dto.setFromNickname(fromUser.getNickname());
                dto.setFromAvatar(fromUser.getAvatar());
            }
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<FriendRequestDTO> getSentRequests(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getFromUserId, userId)
               .orderByDesc(FriendRequest::getCreateTime);
        
        List<FriendRequest> requests = friendRequestMapper.selectList(wrapper);
        return requests.stream().map(req -> {
            FriendRequestDTO dto = new FriendRequestDTO();
            dto.setId(String.valueOf(req.getId()));
            dto.setFromUserId(String.valueOf(req.getFromUserId()));
            dto.setToUserId(String.valueOf(req.getToUserId()));
            dto.setMessage(req.getMessage());
            dto.setStatus(req.getStatus());
            dto.setCreateTime(req.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public int getPendingRequestCount(Long userId) {
        LambdaQueryWrapper<FriendRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FriendRequest::getToUserId, userId)
               .eq(FriendRequest::getStatus, 0);
        return Math.toIntExact(friendRequestMapper.selectCount(wrapper));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException("无权处理该请求");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException("该请求已处理");
        }
        
        // 更新请求状态
        request.setStatus(1);
        friendRequestMapper.updateById(request);
        
        // 创建双向好友关系
        createFriendship(request.getFromUserId(), request.getToUserId());
        createFriendship(request.getToUserId(), request.getFromUserId());
        
        log.info("User {} accepted friend request from user {}", userId, request.getFromUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(Long requestId, Long userId) {
        FriendRequest request = friendRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException("好友请求不存在");
        }
        if (!request.getToUserId().equals(userId)) {
            throw new BusinessException("无权处理该请求");
        }
        if (request.getStatus() != 0) {
            throw new BusinessException("该请求已处理");
        }
        
        request.setStatus(2);
        friendRequestMapper.updateById(request);
        
        log.info("User {} rejected friend request from user {}", userId, request.getFromUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long userId, Long friendId) {
        // 删除双向好友关系
        LambdaQueryWrapper<Friendship> wrapper1 = new LambdaQueryWrapper<>();
        wrapper1.eq(Friendship::getUserId, userId)
                .eq(Friendship::getFriendId, friendId);
        friendshipMapper.delete(wrapper1);
        
        LambdaQueryWrapper<Friendship> wrapper2 = new LambdaQueryWrapper<>();
        wrapper2.eq(Friendship::getUserId, friendId)
                .eq(Friendship::getFriendId, userId);
        friendshipMapper.delete(wrapper2);
        
        log.info("User {} deleted friend {}", userId, friendId);
    }
    
    @Override
    public boolean isFriend(Long userId, Long friendId) {
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getUserId, userId)
               .eq(Friendship::getFriendId, friendId)
               .eq(Friendship::getStatus, 1);
        return friendshipMapper.selectCount(wrapper) > 0;
    }
    
    private void createFriendship(Long userId, Long friendId) {
        // 检查是否已存在
        LambdaQueryWrapper<Friendship> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friendship::getUserId, userId)
               .eq(Friendship::getFriendId, friendId);
        if (friendshipMapper.selectCount(wrapper) > 0) {
            // 更新状态
            Friendship existing = friendshipMapper.selectOne(wrapper);
            existing.setStatus(1);
            friendshipMapper.updateById(existing);
        } else {
            Friendship friendship = new Friendship();
            friendship.setUserId(userId);
            friendship.setFriendId(friendId);
            friendship.setStatus(1);
            friendshipMapper.insert(friendship);
        }
    }
    
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(String.valueOf(user.getId()));
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        return dto;
    }
}
