package com.collab.user.controller;

import com.collab.common.dto.FriendRequestDTO;
import com.collab.common.dto.SendFriendRequest;
import com.collab.common.dto.UserDTO;
import com.collab.common.result.Result;
import com.collab.user.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友控制器
 */
@RestController
@RequestMapping("/friend")
@RequiredArgsConstructor
public class FriendController {
    
    private final FriendService friendService;
    
    /**
     * 获取好友列表
     */
    @GetMapping("/list")
    public Result<List<UserDTO>> getFriendList(@RequestHeader("X-User-Id") Long userId) {
        List<UserDTO> friends = friendService.getFriendList(userId);
        return Result.success(friends);
    }
    
    /**
     * 发送好友请求
     */
    @PostMapping("/request")
    public Result<Void> sendFriendRequest(@RequestHeader("X-User-Id") Long userId,
                                          @RequestBody SendFriendRequest request) {
        friendService.sendFriendRequest(userId, request);
        return Result.success("好友请求已发送", null);
    }
    
    /**
     * 获取收到的好友请求
     */
    @GetMapping("/requests/received")
    public Result<List<FriendRequestDTO>> getReceivedRequests(@RequestHeader("X-User-Id") Long userId) {
        List<FriendRequestDTO> requests = friendService.getReceivedRequests(userId);
        return Result.success(requests);
    }
    
    /**
     * 获取发送的好友请求
     */
    @GetMapping("/requests/sent")
    public Result<List<FriendRequestDTO>> getSentRequests(@RequestHeader("X-User-Id") Long userId) {
        List<FriendRequestDTO> requests = friendService.getSentRequests(userId);
        return Result.success(requests);
    }
    
    /**
     * 获取待处理的好友请求数量
     */
    @GetMapping("/requests/pending/count")
    public Result<Map<String, Integer>> getPendingRequestCount(@RequestHeader("X-User-Id") Long userId) {
        int count = friendService.getPendingRequestCount(userId);
        return Result.success(Map.of("count", count));
    }
    
    /**
     * 同意好友请求
     */
    @PostMapping("/request/{requestId}/accept")
    public Result<Void> acceptRequest(@PathVariable("requestId") String requestId,
                                      @RequestHeader("X-User-Id") Long userId) {
        friendService.acceptRequest(Long.parseLong(requestId), userId);
        return Result.success("已添加好友", null);
    }
    
    /**
     * 拒绝好友请求
     */
    @PostMapping("/request/{requestId}/reject")
    public Result<Void> rejectRequest(@PathVariable("requestId") String requestId,
                                      @RequestHeader("X-User-Id") Long userId) {
        friendService.rejectRequest(Long.parseLong(requestId), userId);
        return Result.success("已拒绝请求", null);
    }
    
    /**
     * 删除好友
     */
    @DeleteMapping("/{friendId}")
    public Result<Void> deleteFriend(@PathVariable("friendId") String friendId,
                                     @RequestHeader("X-User-Id") Long userId) {
        friendService.deleteFriend(userId, Long.parseLong(friendId));
        return Result.success("已删除好友", null);
    }
    
    /**
     * 检查是否为好友
     */
    @GetMapping("/check/{targetUserId}")
    public Result<Map<String, Boolean>> checkFriend(@PathVariable("targetUserId") String targetUserId,
                                                    @RequestHeader("X-User-Id") Long userId) {
        boolean isFriend = friendService.isFriend(userId, Long.parseLong(targetUserId));
        return Result.success(Map.of("isFriend", isFriend));
    }
}
