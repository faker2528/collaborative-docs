<template>
  <div class="message-center">
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="message-tabs">
      <el-tab-pane name="chat">
        <template #label>
          <span>私信消息</span>
          <el-badge v-if="totalUnread > 0" :value="totalUnread" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane name="friendRequest">
        <template #label>
          <span>好友请求</span>
          <el-badge v-if="pendingRequests.length > 0" :value="pendingRequests.length" class="tab-badge" />
        </template>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 私信消息面板 -->
    <el-row v-show="activeTab === 'chat'" :gutter="20" class="chat-panel">
      <!-- 左侧：会话列表 -->
      <el-col :span="8">
        <el-card class="conversation-list-card">
          <template #header>
            <div class="card-header">
              <span>消息列表</span>
              <el-badge :value="totalUnread" :hidden="totalUnread === 0" />
            </div>
          </template>
          
          <el-empty v-if="conversations.length === 0" description="暂无消息" />
          
          <div v-else class="conversation-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conversation-item"
              :class="{ active: selectedConversation?.id === conv.id }"
              @click="selectConversation(conv)"
            >
              <el-avatar 
                :size="48" 
                class="clickable-avatar"
                @click.stop="goToUserProfile(conv.targetUserId)"
              >
                {{ conv.targetNickname?.charAt(0) || conv.targetUsername?.charAt(0) || '?' }}
              </el-avatar>
              <div class="conv-info">
                <div class="conv-header">
                  <span class="conv-name">{{ conv.targetNickname || conv.targetUsername || '未知用户' }}</span>
                  <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
                </div>
                <div class="conv-preview">
                  {{ conv.lastMessageContent || '暂无消息' }}
                </div>
              </div>
              <el-badge 
                v-if="conv.unreadCount > 0" 
                :value="conv.unreadCount" 
                class="unread-badge" 
              />
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：聊天详情 -->
      <el-col :span="16">
        <el-card class="chat-card" v-if="selectedConversation">
          <template #header>
            <div class="chat-header">
              <el-avatar 
                :size="40"
                class="clickable-avatar"
                @click="goToUserProfile(selectedConversation.targetUserId)"
              >
                {{ selectedConversation.targetNickname?.charAt(0) || selectedConversation.targetUsername?.charAt(0) }}
              </el-avatar>
              <span class="chat-name">
                {{ selectedConversation.targetNickname || selectedConversation.targetUsername }}
              </span>
            </div>
          </template>
          
          <!-- 消息列表 -->
          <div class="message-list" ref="messageListRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              class="message-item"
              :class="{ self: msg.isSelf }"
            >
              <el-avatar 
                v-if="!msg.isSelf" 
                :size="36"
                class="msg-avatar clickable-avatar"
                @click="goToUserProfile(msg.senderId)"
              >
                {{ msg.senderNickname?.charAt(0) || msg.senderUsername?.charAt(0) }}
              </el-avatar>
              <div class="msg-content">
                <div class="msg-bubble">{{ msg.content }}</div>
                <div class="msg-time">{{ formatMessageTime(msg.createTime) }}</div>
              </div>
              <el-avatar 
                v-if="msg.isSelf" 
                :size="36"
                class="msg-avatar"
              >
                {{ userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
              </el-avatar>
            </div>
          </div>
          
          <!-- 输入区域 -->
          <div class="input-area">
            <el-input
              v-model="messageInput"
              type="textarea"
              :rows="3"
              placeholder="输入消息，按Enter发送"
              @keydown.enter.exact.prevent="sendMessage"
            />
            <div class="input-actions">
              <el-button type="primary" @click="sendMessage" :disabled="!messageInput.trim()">
                发送
              </el-button>
            </div>
          </div>
        </el-card>
        
        <el-card v-else class="chat-card empty-chat">
          <el-empty description="选择一个会话开始聊天" />
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 好友请求面板 -->
    <div v-show="activeTab === 'friendRequest'" class="friend-request-panel">
      <el-card>
        <template #header>
          <span>收到的好友请求</span>
        </template>
        
        <el-empty v-if="pendingRequests.length === 0" description="暂无好友请求" />
        
        <div v-else class="request-list">
          <div v-for="req in pendingRequests" :key="req.id" class="request-item">
            <el-avatar 
              :size="48" 
              class="clickable-avatar"
              @click="goToUserProfile(req.fromUserId)"
            >
              {{ req.fromNickname?.charAt(0) || req.fromUsername?.charAt(0) || '?' }}
            </el-avatar>
            <div class="request-info">
              <div class="request-name">{{ req.fromNickname || req.fromUsername }}</div>
              <div class="request-message">{{ req.message || '请求添加您为好友' }}</div>
              <div class="request-time">{{ formatTime(req.createTime) }}</div>
            </div>
            <div class="request-actions">
              <el-button type="primary" size="small" @click="handleAccept(req)">同意</el-button>
              <el-button size="small" @click="handleReject(req)">拒绝</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { 
  getConversationList, 
  getMessageList, 
  sendMessage as sendMessageApi,
  markConversationRead 
} from '@/api/message'
import { getReceivedRequests, acceptFriendRequest, rejectFriendRequest } from '@/api/friend'
import { eventBus, Events } from '@/utils/eventBus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('chat')
const conversations = ref([])
const selectedConversation = ref(null)
const messages = ref([])
const messageInput = ref('')
const messageListRef = ref(null)
const loading = ref(false)
const pendingRequests = ref([])

const totalUnread = computed(() => {
  return conversations.value.reduce((sum, conv) => sum + (conv.unreadCount || 0), 0)
})

onMounted(() => {
  fetchConversations()
  fetchPendingRequests()
  
  // 监听新消息事件
  eventBus.on(Events.NEW_MESSAGE, handleNewMessage)
  eventBus.on(Events.FRIEND_REQUEST_CHANGED, fetchPendingRequests)
})

onUnmounted(() => {
  eventBus.off(Events.NEW_MESSAGE, handleNewMessage)
  eventBus.off(Events.FRIEND_REQUEST_CHANGED, fetchPendingRequests)
})

// 处理新消息
function handleNewMessage(newMsg) {
  // 刷新会话列表
  fetchConversations()
  
  // 如果当前选中的会话是新消息的会话，追加消息
  if (selectedConversation.value && 
      String(newMsg.senderId) === String(selectedConversation.value.targetUserId)) {
    messages.value.push(newMsg)
    nextTick(() => scrollToBottom())
    // 自动标记已读
    markConversationRead(selectedConversation.value.id)
    selectedConversation.value.unreadCount = 0
    eventBus.emit(Events.UNREAD_COUNT_CHANGED)
  }
}

async function fetchConversations() {
  try {
    const res = await getConversationList()
    if (res.code === 200) {
      conversations.value = res.data || []
    }
  } catch (e) {
    console.error('获取会话列表失败:', e)
  }
}

async function fetchPendingRequests() {
  try {
    const res = await getReceivedRequests()
    if (res.code === 200) {
      // 过滤出未处理的请求
      pendingRequests.value = (res.data || []).filter(r => r.status === 0)
    }
  } catch (e) {
    console.error('获取好友请求失败:', e)
  }
}

async function selectConversation(conv) {
  selectedConversation.value = conv
  await fetchMessages()
  
  // 标记已读
  if (conv.unreadCount > 0) {
    try {
      await markConversationRead(conv.id)
      conv.unreadCount = 0
      // 通知Layout刷新未读数
      eventBus.emit(Events.UNREAD_COUNT_CHANGED)
    } catch (e) {
      console.error('标记已读失败:', e)
    }
  }
}

async function fetchMessages() {
  if (!selectedConversation.value) return
  
  try {
    const res = await getMessageList(selectedConversation.value.id, 1, 50)
    if (res.code === 200) {
      messages.value = (res.data || []).reverse() // 倒序显示
      await nextTick()
      scrollToBottom()
    }
  } catch (e) {
    console.error('获取消息列表失败:', e)
  }
}

async function sendMessage() {
  if (!messageInput.value.trim() || !selectedConversation.value) return
  
  const content = messageInput.value.trim()
  messageInput.value = ''
  
  try {
    const res = await sendMessageApi({
      receiverId: selectedConversation.value.targetUserId,
      content: content,
      type: 1
    })
    
    if (res.code === 200) {
      messages.value.push(res.data)
      await nextTick()
      scrollToBottom()
      
      // 更新会话列表
      selectedConversation.value.lastMessageContent = content
      selectedConversation.value.lastMessageTime = new Date().toISOString()
    }
  } catch (e) {
    ElMessage.error('发送失败')
    messageInput.value = content // 恢复输入
  }
}

async function handleAccept(req) {
  try {
    const res = await acceptFriendRequest(req.id)
    if (res.code === 200) {
      ElMessage.success('已同意好友请求')
      // 从列表移除
      pendingRequests.value = pendingRequests.value.filter(r => r.id !== req.id)
      eventBus.emit(Events.FRIEND_REQUEST_CHANGED)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleReject(req) {
  try {
    const res = await rejectFriendRequest(req.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝好友请求')
      pendingRequests.value = pendingRequests.value.filter(r => r.id !== req.id)
      eventBus.emit(Events.FRIEND_REQUEST_CHANGED)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function goToUserProfile(userId) {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  
  return date.toLocaleDateString('zh-CN')
}

function formatMessageTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.message-center {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
  
  .message-tabs {
    margin-bottom: 16px;
    
    .tab-badge {
      margin-left: 6px;
    }
  }
  
  .chat-panel {
    flex: 1;
    min-height: 0;
    
    .el-col {
      height: 100%;
    }
  }
  
  .friend-request-panel {
    flex: 1;
  }
}

.conversation-list-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  :deep(.el-card__body) {
    flex: 1;
    overflow-y: auto;
    padding: 0;
  }
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.conversation-list {
  .conversation-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    cursor: pointer;
    transition: background-color 0.2s;
    border-bottom: 1px solid #f0f0f0;
    
    &:hover {
      background-color: #f5f7fa;
    }
    
    &.active {
      background-color: #ecf5ff;
    }
    
    .conv-info {
      flex: 1;
      margin-left: 12px;
      overflow: hidden;
      
      .conv-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .conv-name {
          font-weight: 500;
          color: #303133;
        }
        
        .conv-time {
          font-size: 12px;
          color: #909399;
        }
      }
      
      .conv-preview {
        margin-top: 4px;
        font-size: 13px;
        color: #909399;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
    
    .unread-badge {
      margin-left: 8px;
    }
  }
}

.chat-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  
  :deep(.el-card__body) {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    padding: 0;
  }
  
  &.empty-chat {
    :deep(.el-card__body) {
      justify-content: center;
      align-items: center;
    }
  }
  
  .chat-header {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .chat-name {
      font-weight: 500;
      font-size: 16px;
    }
  }
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background-color: #f5f7fa;
  
  .message-item {
    display: flex;
    align-items: flex-start;
    margin-bottom: 16px;
    
    &.self {
      flex-direction: row-reverse;
      
      .msg-content {
        align-items: flex-end;
        
        .msg-bubble {
          background-color: #409eff;
          color: #fff;
        }
      }
    }
    
    .msg-avatar {
      flex-shrink: 0;
    }
    
    .msg-content {
      display: flex;
      flex-direction: column;
      margin: 0 10px;
      max-width: 60%;
      
      .msg-bubble {
        background-color: #fff;
        padding: 10px 14px;
        border-radius: 8px;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
        word-wrap: break-word;
        line-height: 1.5;
      }
      
      .msg-time {
        margin-top: 4px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.input-area {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
  background-color: #fff;
  
  .input-actions {
    margin-top: 12px;
    text-align: right;
  }
}

.clickable-avatar {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: scale(1.05);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}

// 好友请求样式
.request-list {
  .request-item {
    display: flex;
    align-items: center;
    padding: 16px;
    border-bottom: 1px solid #f0f0f0;
    
    &:last-child {
      border-bottom: none;
    }
    
    .request-info {
      flex: 1;
      margin-left: 16px;
      
      .request-name {
        font-weight: 500;
        color: #303133;
        margin-bottom: 4px;
      }
      
      .request-message {
        font-size: 13px;
        color: #606266;
        margin-bottom: 4px;
      }
      
      .request-time {
        font-size: 12px;
        color: #909399;
      }
    }
    
    .request-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style>
