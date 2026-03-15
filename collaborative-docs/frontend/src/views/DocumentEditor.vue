<template>
  <div class="document-editor-container">
    <!-- 顶部工具栏 -->
    <header class="editor-header">
      <div class="header-content">
        <div class="header-left">
          <div class="nav-group">
            <el-button link circle @click="goBack" class="back-btn">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <div class="title-wrapper">
              <input 
                v-model="documentTitle" 
                class="doc-title-input"
                placeholder="无标题文档"
                @blur="saveTitle"
              />
              <div class="status-indicator">
                <el-tag 
                  v-if="connectionStatus === 'connected'" 
                  type="success" 
                  size="small"
                  effect="light"
                  round
                >
                  <el-icon><Connection /></el-icon>
                  已连接
                </el-tag>
                <el-tag 
                  v-else-if="connectionStatus === 'connecting'" 
                  type="warning" 
                  size="small"
                  effect="light"
                  round
                >
                  <el-icon><Loading /></el-icon>
                  连接中
                </el-tag>
                <el-tag 
                  v-else 
                  type="danger" 
                  size="small"
                  effect="light"
                  round
                >
                  <el-icon><Close /></el-icon>
                  未连接
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <div class="header-center">
          <div class="online-users-wrapper">
            <div class="users-label">
              <el-icon class="label-icon"><UserFilled /></el-icon>
              <span class="label-text">在线协作</span>
              <el-tag type="primary" size="small" effect="plain" class="count-tag">
                {{ onlineUsers.length + 1 }}
              </el-tag>
            </div>
            <div class="avatar-list">
              <el-tooltip 
                v-for="user in onlineUsers" 
                :key="user.siteId"
                :content="user.username"
                placement="bottom"
              >
                <el-avatar 
                  :size="36"
                  class="online-avatar"
                  @click="goToUserProfile(user.userId)"
                >
                  {{ user.username?.charAt(0) }}
                </el-avatar>
              </el-tooltip>
              <el-tooltip 
                content="我"
                placement="bottom"
              >
                <el-avatar 
                  :size="36"
                  class="online-avatar self"
                >
                  {{ userStore.userProfile?.nickname?.charAt(0) || userStore.userProfile?.username?.charAt(0) }}
                </el-avatar>
              </el-tooltip>
            </div>
          </div>
        </div>
        
        <div class="header-right">
          <div class="action-buttons">
            <el-button 
              type="primary" 
              size="default" 
              @click="saveDocument" 
              :loading="saving"
              class="save-btn"
              round
            >
              <el-icon><DocumentChecked /></el-icon>
              <span>{{ saving ? '保存中...' : '保存' }}</span>
            </el-button>
            <el-button 
              type="primary" 
              size="default"
              @click="toggleCommentPanel"
              class="panel-btn"
              round
            >
              <el-icon><ChatDotRound /></el-icon>
              <span>评论</span>
              <el-badge :value="comments.length" :hidden="comments.length === 0" size="small" />
            </el-button>
            <el-button 
              type="success" 
              size="default"
              @click="toggleMemberPanel"
              class="panel-btn"
              round
            >
              <el-icon><User /></el-icon>
              <span>成员</span>
              <el-badge :value="onlineUsers.length + members.length" size="small" />
            </el-button>
            <el-button 
              type="warning" 
              size="default" 
              @click="showInviteDialog = true"
              class="invite-btn"
              round
            >
              <el-icon><Share /></el-icon>
              <span>邀请协作</span>
            </el-button>
            <el-dropdown trigger="click">
              <el-button size="default" class="more-btn" circle>
                <el-icon><Grid /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="showMembersDialog = true">
                    <el-icon><User /></el-icon>
                    成员管理
                  </el-dropdown-item>
                  <el-dropdown-item @click="showVersionDialog = true">
                    <el-icon><Clock /></el-icon>
                    版本历史
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="deleteDocument">
                    <el-icon><Delete /></el-icon>
                    删除文档
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>
    </header>
    
    <!-- 编辑器主体 -->
    <div class="editor-body">
      <div class="editor-card">
        <div class="toolbar-wrapper">
          <div ref="toolbarRef" class="quill-toolbar"></div>
        </div>
        <div class="editor-wrapper">
          <div ref="editorRef" class="quill-editor"></div>
        </div>
      </div>
    </div>
    
    <!-- 悬浮面板（评论/成员） -->
    <transition name="slide-right">
      <aside class="floating-panel" v-if="showFloatingPanel">
        <div class="panel-header">
          <div class="header-title">
            <el-icon class="title-icon">{{ currentPanel === 'comments' ? ChatDotRound : User }}</el-icon>
            <span>{{ currentPanel === 'comments' ? '评论' : '成员' }}</span>
            <el-badge 
              :value="currentPanel === 'comments' ? comments.length : onlineUsers.length + members.length" 
              size="small" 
            />
          </div>
          <el-button link circle @click="closeFloatingPanel" class="close-btn">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        
        <div class="panel-content-wrapper">
          <!-- 评论面板 -->
          <div v-show="currentPanel === 'comments'" class="panel-content comments-panel">
            <div class="comment-input-wrapper">
              <el-input
                v-model="newComment"
                type="textarea"
                :rows="2"
                placeholder="写下你的评论..."
                class="comment-input"
                @keydown.ctrl.enter="submitComment"
              />
              <div class="comment-actions">
                <span class="hint-text">Ctrl+Enter 快速发送</span>
                <el-button 
                  type="primary" 
                  @click="submitComment"
                  :disabled="!newComment.trim()"
                  :loading="saving"
                  size="default"
                >
                  发送
                </el-button>
              </div>
            </div>
            
            <el-divider border-style="dashed" style="margin: 16px 0;" />
            
            <div class="comment-list">
              <transition-group name="comment-list">
                <div 
                  v-for="comment in comments" 
                  :key="comment.id" 
                  class="comment-item"
                >
                  <el-avatar 
                    :size="36" 
                    class="comment-avatar" 
                    @click="goToUserProfile(comment.userId)"
                    :src="comment.avatarUrl"
                  >
                    {{ comment.username?.charAt(0) }}
                  </el-avatar>
                  <div class="comment-body">
                    <div class="comment-header">
                      <span class="comment-author">{{ comment.username }}</span>
                      <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                    </div>
                    <div class="comment-content">{{ comment.text }}</div>
                    <div class="comment-footer">
                      <el-button 
                        link 
                        size="small" 
                        @click="toggleReply(comment)"
                        class="reply-btn"
                      >
                        回复
                      </el-button>
                      <el-button 
                        link 
                        size="small" 
                        @click="likeComment(comment)"
                        :type="comment.liked ? 'danger' : 'primary'"
                        :class="{ 'liked': comment.liked }"
                        class="like-btn"
                      >
                        <el-icon><Promotion /></el-icon>
                        {{ comment.likeCount || 0 }}
                      </el-button>
                      <el-button 
                        v-if="isCommentOwner(comment)" 
                        link 
                        size="small" 
                        @click="deleteComment(comment)"
                        type="danger"
                        class="delete-btn"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                    
                    <!-- 回复输入框 -->
                    <div v-if="comment.showReplyInput" class="reply-input-wrapper">
                      <el-input
                        v-model="comment.replyText"
                        type="textarea"
                        :rows="2"
                        placeholder="写下你的回复..."
                        @keydown.ctrl.enter="submitReply(comment)"
                      />
                      <div class="reply-actions">
                        <el-button size="small" @click="cancelReply(comment)">取消</el-button>
                        <el-button 
                          type="primary" 
                          size="small" 
                          @click="submitReply(comment)"
                          :loading="saving"
                        >
                          发送
                        </el-button>
                      </div>
                    </div>
                    
                    <!-- 回复列表（抖音风格：扁平化展示） -->
                    <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                      <div 
                        v-for="reply in comment.replies" 
                        :key="reply.id" 
                        class="reply-item"
                      >
                        <el-avatar 
                          :size="28" 
                          class="reply-avatar"
                          @click="goToUserProfile(reply.userId)"
                          :src="reply.avatarUrl"
                        >
                          {{ reply.username?.charAt(0) }}
                        </el-avatar>
                        <div class="reply-main">
                          <div class="reply-header">
                            <span class="reply-author">{{ reply.username }}</span>
                            <!-- 如果回复的是子评论，显示箭头和被回复者 -->
                            <template v-if="reply.replyToUsername && reply.replyToId !== reply.parentId">
                              <el-icon class="reply-arrow"><CaretRight /></el-icon>
                              <span class="reply-to-user">{{ reply.replyToUsername }}</span>
                            </template>
                          </div>
                          <div class="reply-text">{{ reply.text }}</div>
                          <div class="reply-footer">
                            <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                            <el-button 
                              link 
                              size="small" 
                              @click="toggleReplyToReply(comment, reply)"
                              class="reply-btn"
                            >
                              回复
                            </el-button>
                            <el-button 
                              link 
                              size="small" 
                              @click="likeComment(reply)"
                              :type="reply.liked ? 'danger' : 'primary'"
                              :class="{ 'liked': reply.liked }"
                              class="like-btn"
                            >
                              <el-icon><Promotion /></el-icon>
                              {{ reply.likeCount || 0 }}
                            </el-button>
                            <el-button 
                              v-if="isCommentOwner(reply)" 
                              link 
                              size="small" 
                              @click="deleteReply(comment, reply)"
                              type="danger"
                              class="delete-btn"
                            >
                              删除
                            </el-button>
                          </div>
                          
                          <!-- 回复的回复输入框 -->
                          <div v-if="reply.showReplyInput" class="reply-input-wrapper">
                            <el-input
                              v-model="reply.replyText"
                              type="textarea"
                              :rows="2"
                              :placeholder="`回复 ${reply.username}...`"
                              @keydown.ctrl.enter="submitReplyToReply(comment, reply)"
                            />
                            <div class="reply-actions">
                              <el-button size="small" @click="cancelReplyToReply(reply)">取消</el-button>
                              <el-button 
                                type="primary" 
                                size="small" 
                                @click="submitReplyToReply(comment, reply)"
                                :loading="saving"
                              >
                                发送
                              </el-button>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </transition-group>
              <el-empty v-if="comments.length === 0" description="暂无评论" :image-size="60" />
            </div>
          </div>
          
          <!-- 成员面板 -->
          <div v-show="currentPanel === 'members'" class="panel-content members-panel">
            <!-- 在线用户 -->
            <div class="panel-section">
              <div class="section-title">
                <el-icon class="section-icon"><Connection /></el-icon>
                <span>在线成员 ({{ onlineUsers.length }})</span>
              </div>
              <div class="user-list">
                <div 
                  v-for="user in onlineUsers" 
                  :key="user.userId" 
                  class="user-item"
                >
                  <el-avatar :size="36" class="user-avatar">{{ user.username.charAt(0) }}</el-avatar>
                  <div class="user-info">
                    <div class="user-name">{{ user.username }}</div>
                    <div class="user-status">正在编辑</div>
                  </div>
                  <el-tag type="success" size="small" effect="plain">在线</el-tag>
                </div>
                <el-empty v-if="onlineUsers.length === 0" description="暂无其他在线用户" :image-size="50" />
              </div>
            </div>
            
            <el-divider style="margin: 16px 0;" />
            
            <!-- 协作者列表 -->
            <div class="panel-section">
              <div class="section-title">
                <el-icon class="section-icon"><UserFilled /></el-icon>
                <span>协作者 ({{ members.length }})</span>
              </div>
              <div class="user-list">
                <div 
                  v-for="member in members" 
                  :key="member.userId" 
                  class="user-item"
                >
                  <el-avatar :size="36" class="user-avatar">{{ member.username.charAt(0) }}</el-avatar>
                  <div class="user-info">
                    <div class="user-name">{{ member.username }}</div>
                    <div class="user-permission">
                      <el-tag 
                        :type="member.permissionType === 2 ? 'success' : 'info'" 
                        size="small"
                        effect="light"
                      >
                        {{ member.permissionType === 2 ? '可编辑' : '只读' }}
                      </el-tag>
                    </div>
                  </div>
                  <el-select 
                    v-if="isCreator" 
                    v-model="member.permissionType" 
                    size="small"
                    @change="updatePermission(member)"
                    class="permission-select"
                  >
                    <el-option label="只读" :value="1" />
                    <el-option label="可编辑" :value="2" />
                  </el-select>
                </div>
                <el-empty v-if="members.length === 0" description="暂无协作者" :image-size="50" />
              </div>
            </div>
          </div>
        </div>
      </aside>
    </transition>
    
    <!-- 邀请协作对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请协作" width="600px">
      <el-tabs v-model="inviteTab">
        <el-tab-pane label="邀请好友" name="friend">
          <div class="invite-section">
            <el-empty v-if="friends.length === 0" description="暂无好友">
              <el-button type="primary" @click="$router.push('/friends')">去添加好友</el-button>
            </el-empty>
            <div v-else class="friend-grid">
              <el-card 
                v-for="friend in friends" 
                :key="friend.id"
                shadow="hover"
                class="friend-card"
                :class="{ selected: inviteForm.selectedUser?.id === friend.id }"
                @click="selectFriend(friend)"
              >
                <div class="friend-card-content">
                  <el-avatar :size="40">{{ friend.username.charAt(0) }}</el-avatar>
                  <div class="friend-card-info">
                    <div class="friend-name">{{ friend.nickname || friend.username }}</div>
                    <div class="friend-username">@{{ friend.username }}</div>
                  </div>
                  <el-icon v-if="inviteForm.selectedUser?.id === friend.id" class="check-icon"><Check /></el-icon>
                </div>
              </el-card>
            </div>
            
            <el-form v-if="inviteForm.selectedUser && isCreator" style="margin-top: 16px;">
              <el-form-item label="权限类型">
                <el-radio-group v-model="inviteForm.permissionType">
                  <el-radio :value="1">只读</el-radio>
                  <el-radio :value="2">可编辑</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="分享链接" name="link">
          <div class="invite-section">
            <el-form :model="shareLinkForm" label-width="100px">
              <el-form-item label="权限类型" v-if="isCreator">
                <el-radio-group v-model="shareLinkForm.permissionType">
                  <el-radio :value="1">只读</el-radio>
                  <el-radio :value="2">可编辑</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-alert 
                v-else 
                type="info" 
                :closable="false"
                show-icon
                style="margin-bottom: 16px;"
              >
                您不是文档创建者，生成的链接只能为只读权限
              </el-alert>
              
              <el-form-item label="有效期">
                <el-select v-model="shareLinkForm.validDays" style="width: 100%;">
                  <el-option :value="0" label="永久有效" />
                  <el-option :value="1" label="1 天" />
                  <el-option :value="7" label="7 天" />
                  <el-option :value="30" label="30 天" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="使用次数">
                <el-select v-model="shareLinkForm.maxUses" style="width: 100%;">
                  <el-option :value="0" label="不限制" />
                  <el-option :value="1" label="1 次" />
                  <el-option :value="5" label="5 次" />
                  <el-option :value="10" label="10 次" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="验证类型">
                <el-select v-model="shareLinkForm.verificationType" style="width: 100%;">
                  <el-option :value="0" label="免验证" />
                  <el-option :value="1" label="密码验证" />
                  <el-option :value="2" label="邮箱验证" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="密码" v-if="shareLinkForm.verificationType === 1">
                <el-input 
                  v-model="shareLinkForm.password" 
                  type="password"
                  placeholder="设置访问密码"
                  show-password
                />
              </el-form-item>
              
              <el-form-item label="邮箱" v-if="shareLinkForm.verificationType === 2">
                <el-input 
                  v-model="shareLinkForm.email" 
                  type="email"
                  placeholder="指定邮箱地址"
                />
              </el-form-item>
            </el-form>
            
            <el-result
              v-if="generatedLink"
              icon="success"
              title="链接生成成功"
              sub-title="复制链接发送给协作者"
            >
              <template #extra>
                <el-input v-model="generatedLink" readonly style="margin-bottom: 12px;">
                  <template #append>
                    <el-button @click="copyLink">
                      <el-icon><CopyDocument /></el-icon>
                      复制
                    </el-button>
                  </template>
                </el-input>
              </template>
            </el-result>
          </div>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="showInviteDialog = false">关闭</el-button>
        <el-button 
          v-if="inviteTab === 'friend'" 
          type="primary" 
          @click="inviteFriend"
          :disabled="!inviteForm.selectedUser"
        >
          发送邀请
        </el-button>
        <el-button 
          v-else 
          type="primary" 
          @click="generateShareLink"
          :loading="generatingLink"
        >
          {{ generatedLink ? '重新生成' : '生成链接' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getDocument,
  updateDocument,
  deleteDocument as apiDeleteDocument,
  getDocumentMembers,
  updateMemberPermission as apiUpdateMemberPermission
} from '@/api/document'
import { getFriendList } from '@/api/friend'
import { createShareLink } from '@/api/document'
import { getComments, addComment as apiAddComment, deleteComment as apiDeleteComment, replyComment as apiReplyComment, likeComment as apiLikeComment } from '@/api/comment'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'

// Element Plus 图标
import {
  ArrowLeft, Connection, Loading, Close, UserFilled,
  DocumentChecked, Share, Grid, User, Clock, Delete,
  ChatDotRound, Check, CopyDocument, Promotion, CaretRight
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ========== 文档数据 ==========
const documentTitle = ref('')
const documentContent = ref(null)
const saving = ref(false)
const isCreator = ref(false)

// ========== WebSocket 连接 ==========
const connectionStatus = ref('unconnected')
const onlineUsers = ref([])
let ws = null
let reconnectTimer = null
const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/collaboration'

// ========== 编辑器 ==========
const editorRef = ref(null)
const toolbarRef = ref(null)
let quillEditor = null

// ========== 悬浮面板 ==========
const showFloatingPanel = ref(false)
const currentPanel = ref('comments')

// ========== 评论 ==========
const newComment = ref('')
const comments = ref([])

// ========== 成员 ==========
const members = ref([])

// ========== 邀请与分享 ==========
const showInviteDialog = ref(false)
const inviteTab = ref('friend')
const inviteForm = reactive({
  selectedUser: null,
  permissionType: 1
})
const friends = ref([])
const shareLinkForm = reactive({
  permissionType: 1,
  validDays: 0,
  maxUses: 0,
  verificationType: 0,
  password: '',
  email: ''
})
const generatedLink = ref('')
const generatingLink = ref(false)

// ========== 其他对话框 ==========
const showMembersDialog = ref(false)
const showVersionDialog = ref(false)

// ========== 生命周期 ==========
onMounted(async () => {
  await loadDocument()
  await nextTick()
  initEditor()
  await loadComments()
  await loadMembers()
  await loadFriends()
  connectWebSocket()
})

onUnmounted(() => {
  disconnectWebSocket()
  if (reconnectTimer) clearTimeout(reconnectTimer)
})

// ========== 文档加载与保存 ==========
async function loadDocument() {
  try {
    const res = await getDocument(route.params.id)
    if (res.code === 200 && res.data) {
      documentTitle.value = res.data.title
      documentContent.value = JSON.parse(res.data.content || '{}')
      isCreator.value = res.data.creatorId === userStore.userProfile?.userId
    } else {
      ElMessage.error(res.msg || '文档不存在')
    }
  } catch (e) {
    console.error('加载文档失败:', e)
    ElMessage.error('加载文档失败')
  }
}

function initEditor() {
  if (!editorRef.value) {
    ElMessage.error('编辑器初始化失败')
    return
  }
  
  quillEditor = new Quill(editorRef.value, {
    theme: 'snow',
    modules: {
      toolbar: [
        [{ 'header': [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        [{ 'list': 'ordered'}, { 'list': 'bullet' }],
        [{ 'color': [] }, { 'background': [] }],
        ['link', 'image'],
        ['clean']
      ]
    }
  })
  
  if (documentContent.value) {
    quillEditor.setContents(documentContent.value)
  }
  
  quillEditor.on('text-change', () => {
    debouncedSave()
  })
}

let saveTimer = null
function debouncedSave() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => saveDocument(), 2000)
}

async function saveDocument() {
  if (!quillEditor) return
  
  saving.value = true
  try {
    const content = JSON.stringify(quillEditor.getContents())
    const res = await updateDocument(route.params.id, content)
    if (res.code === 200) {
      ElMessage.success('保存成功')
    }
  } catch (e) {
    console.error('保存失败:', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

function goToUserProfile(userId) {
  router.push(`/user/${userId}`)
}

// 控制悬浮面板
function toggleCommentPanel() {
  if (showFloatingPanel.value && currentPanel.value === 'comments') {
    closeFloatingPanel()
  } else {
    currentPanel.value = 'comments'
    showFloatingPanel.value = true
  }
}

function toggleMemberPanel() {
  if (showFloatingPanel.value && currentPanel.value === 'members') {
    closeFloatingPanel()
  } else {
    currentPanel.value = 'members'
    showFloatingPanel.value = true
  }
}

function closeFloatingPanel() {
  showFloatingPanel.value = false
}

// ========== WebSocket 实时协作 ==========
function connectWebSocket() {
  try {
    const token = userStore.token
    const wsUrl = `${WS_URL}/${route.params.id}?token=${token}`
    
    ws = new WebSocket(wsUrl)
    
    ws.onopen = () => {
      connectionStatus.value = 'connected'
      console.log('WebSocket 连接成功')
      // 加入文档协作房间
      ws.send(JSON.stringify({
        type: 'join',
        documentId: route.params.id
      }))
    }
    
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data)
      handleMessage(data)
    }
    
    ws.onerror = (error) => {
      console.error('WebSocket 错误:', error)
      connectionStatus.value = 'unconnected'
    }
    
    ws.onclose = () => {
      connectionStatus.value = 'unconnected'
      console.log('WebSocket 连接关闭，3 秒后重连...')
      // 断线重连
      reconnectTimer = setTimeout(() => {
        connectWebSocket()
      }, 3000)
    }
  } catch (error) {
    console.error('WebSocket 连接失败:', error)
    connectionStatus.value = 'unconnected'
  }
}

function disconnectWebSocket() {
  if (ws) {
    ws.close()
    ws = null
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  connectionStatus.value = 'unconnected'
}

function handleMessage(data) {
  switch(data.type) {
    case 'user_joined':
      // 用户加入
      if (!onlineUsers.value.find(u => u.userId === data.user.userId)) {
        onlineUsers.value.push(data.user)
      }
      break
    case 'user_left':
      // 用户离开
      onlineUsers.value = onlineUsers.value.filter(
        u => u.userId !== data.userId
      )
      break
    case 'content_change':
      // 内容变更（CRDT 处理）
      applyRemoteChange(data.change)
      break
    case 'cursor_update':
      // 光标位置更新
      updateRemoteCursor(data.userId, data.cursor)
      break
    case 'COMMENT_ADDED':
    case 'comment_added':
      // 新评论
      handleNewComment(data)
      break
    case 'COMMENT_REPLY':
    case 'comment_reply':
      // 评论回复
      handleCommentReply(data)
      break
  }
}

// 处理新评论
function handleNewComment(data) {
  const comment = data.comment
  if (!comment) return
  
  // 添加到评论列表
  const newComment = {
    ...comment,
    showReplyInput: false,
    replyText: '',
    replies: []
  }
  
  // 检查是否已存在
  if (!comments.value.find(c => c.id === comment.id)) {
    comments.value.unshift(newComment)
    
    // 显示通知
    ElMessage({
      type: 'info',
      message: `${comment.username} 发表了新评论`,
      duration: 3000
    })
  }
}

// 处理评论回复
function handleCommentReply(data) {
  const reply = data.comment
  if (!reply || !reply.parentId) return
  
  // 找到顶级评论
  const parentComment = comments.value.find(c => c.id === reply.parentId)
  if (parentComment) {
    // 检查是否已存在
    if (!parentComment.replies.find(r => r.id === reply.id)) {
      // 添加回复的状态属性
      reply.showReplyInput = false
      reply.replyText = ''
      parentComment.replies.push(reply)
      
      // 排序
      parentComment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
      
      // 显示通知
      ElMessage({
        type: 'info',
        message: `${reply.username} 回复了评论`,
        duration: 3000
      })
    }
  }
}

function applyRemoteChange(change) {
  // TODO: 实现 CRDT 冲突解决算法
  console.log('应用远程变更:', change)
}

function updateRemoteCursor(userId, cursor) {
  // TODO: 显示远程光标
  console.log('用户', userId, '光标位置:', cursor)
}

// ========== 评论功能 ==========
async function loadComments() {
  try {
    const res = await getComments(route.params.id)
    if (res.code === 200) {
      // 构建扁平化结构（抖音风格）
      const allComments = (res.data || []).map(comment => ({
        ...comment,
        showReplyInput: false,
        replyText: '',
        replies: []
      }))
      
      // 分离顶级评论和回复
      const rootComments = allComments.filter(c => !c.parentId)
      const replies = allComments.filter(c => c.parentId)
      
      // 将所有回复挂在对应的顶级评论下（扁平化）
      replies.forEach(reply => {
        const parentComment = rootComments.find(c => c.id === reply.parentId)
        if (parentComment) {
          reply.showReplyInput = false
          reply.replyText = ''
          parentComment.replies.push(reply)
        }
      })
      
      // 按时间排序：顶级评论倒序，回复正序
      rootComments.sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
      rootComments.forEach(comment => {
        comment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
      })
      
      comments.value = rootComments
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  }
}

// 构建树状结构（已弃用，改用扁平化）
function buildTree(rootComments, allReplies) {
  // 递归构建
  function buildReplies(parent) {
    const directReplies = allReplies.filter(r => r.parentId === parent.id)
    directReplies.forEach(reply => {
      // 添加状态属性
      reply.showReplyInput = false
      reply.replyText = ''
      reply.replies = []
      
      parent.replies.push(reply)
      
      // 递归构建子回复
      buildReplies(reply)
    })
    
    // 子回复按时间正序
    parent.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
  }
  
  // 为每个顶级评论构建回复树
  rootComments.forEach(comment => {
    buildReplies(comment)
  })
}

async function submitComment() {
  if (!newComment.value.trim()) return
  
  try {
    const res = await apiAddComment({
      documentId: route.params.id,
      text: newComment.value
    })
    
    if (res.code === 200) {
      const comment = res.data
      comment.showReplyInput = false
      comment.replyText = ''
      comment.replies = []
      comments.value.unshift(comment)
      newComment.value = ''
      ElMessage.success('评论已发布')
    }
  } catch (error) {
    console.error('提交评论失败:', error)
    ElMessage.error('提交评论失败')
  }
}

// 切换回复框
function toggleReply(comment) {
  // 先关闭所有回复的回复框
  if (comment.replies) {
    comment.replies.forEach(r => {
      r.showReplyInput = false
    })
  }
  comment.showReplyInput = !comment.showReplyInput
  if (comment.showReplyInput) {
    comment.replyText = ''
  }
}

// 取消回复
function cancelReply(comment) {
  comment.showReplyInput = false
  comment.replyText = ''
}

// 提交回复（回复父评论）
async function submitReply(comment) {
  if (!comment.replyText.trim()) return
  
  try {
    const res = await apiReplyComment({
      documentId: route.params.id,
      parentId: comment.id,
      replyToId: comment.id,  // 回复父评论时，replyToId 等于 parentId
      text: comment.replyText
    })
    
    if (res.code === 200) {
      if (!comment.replies) {
        comment.replies = []
      }
      // 添加回复的状态属性
      const newReply = res.data
      newReply.showReplyInput = false
      newReply.replyText = ''
      comment.replies.push(newReply)
      comment.replyText = ''
      comment.showReplyInput = false
      
      // 排序
      comment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
      
      ElMessage.success('回复成功')
    }
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复失败')
  }
}

// 切换回复的回复框
function toggleReplyToReply(comment, reply) {
  // 先关闭其他回复框
  comment.replies.forEach(r => {
    if (r.id !== reply.id) {
      r.showReplyInput = false
    }
  })
  comment.showReplyInput = false
  
  // 切换当前回复框
  reply.showReplyInput = !reply.showReplyInput
  if (reply.showReplyInput) {
    reply.replyText = ''
  }
}

// 取消回复的回复
function cancelReplyToReply(reply) {
  reply.showReplyInput = false
  reply.replyText = ''
}

// 提交回复的回复（回复子评论）
async function submitReplyToReply(comment, reply) {
  if (!reply.replyText?.trim()) return
  
  try {
    const res = await apiReplyComment({
      documentId: route.params.id,
      parentId: comment.id,  // 顶级评论ID，用于分组
      replyToId: reply.id,  // 回复目标的ID
      text: reply.replyText
    })
    
    if (res.code === 200) {
      if (!comment.replies) {
        comment.replies = []
      }
      // 添加回复的状态属性
      const newReply = res.data
      newReply.showReplyInput = false
      newReply.replyText = ''
      comment.replies.push(newReply)
      reply.replyText = ''
      reply.showReplyInput = false
      
      // 排序
      comment.replies.sort((a, b) => new Date(a.createTime) - new Date(b.createTime))
      
      ElMessage.success('回复成功')
    }
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复失败')
  }
}

// 点赞评论
async function likeComment(comment) {
  try {
    const res = await apiLikeComment(comment.id)
    if (res.code === 200) {
      // 切换点赞状态
      if (comment.liked) {
        // 取消点赞
        comment.liked = false
        comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
        ElMessage.success('已取消点赞')
      } else {
        // 点赞
        comment.liked = true
        comment.likeCount = (comment.likeCount || 0) + 1
        ElMessage.success('点赞成功')
      }
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除评论
async function deleteComment(comment) {
  try {
    await ElMessageBox.confirm('确定要删除此评论吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteComment(comment.id)
    if (res.code === 200) {
      comments.value = comments.value.filter(c => c.id !== comment.id)
      ElMessage.success('评论已删除')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除评论失败:', error)
      ElMessage.error('删除评论失败')
    }
  }
}

// 删除回复
async function deleteReply(comment, reply) {
  try {
    await ElMessageBox.confirm('确定要删除此回复吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteComment(reply.id)
    if (res.code === 200) {
      // 从父评论的回复列表中移除
      comment.replies = comment.replies.filter(r => r.id !== reply.id)
      ElMessage.success('回复已删除')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除回复失败:', error)
      ElMessage.error('删除回复失败')
    }
  }
}

// 判断是否是自己的评论
function isCommentOwner(comment) {
  if (!comment?.userId || !userStore.userInfo?.userId) return false
  // 统一转为字符串并去除可能的空格
  const commentUserId = String(comment.userId).trim()
  const currentUserId = String(userStore.userInfo.userId).trim()
  console.log('[isCommentOwner] commentUserId:', commentUserId, 'currentUserId:', currentUserId, 'match:', commentUserId === currentUserId)
  return commentUserId === currentUserId
}

// ========== 成员管理 ==========
async function loadMembers() {
  try {
    const res = await getDocumentMembers(route.params.id)
    if (res.code === 200) {
      members.value = res.data?.list || []
    }
  } catch (error) {
    console.error('加载成员失败:', error)
    ElMessage.error('加载成员失败')
  }
}

async function updatePermission(member) {
  try {
    const res = await apiUpdateMemberPermission(
      route.params.id,
      member.userId,
      member.permissionType
    )
    
    if (res.code === 200) {
      ElMessage.success('权限已更新')
    }
  } catch (error) {
    console.error('更新权限失败:', error)
    ElMessage.error('更新权限失败')
    // 恢复原权限（这里简化处理，实际应该保存旧值）
  }
}

// ========== 好友与邀请 ==========
async function loadFriends() {
  try {
    const res = await getFriendList()
    if (res.code === 200) {
      friends.value = res.data?.list || []
    }
  } catch (error) {
    console.error('加载好友列表失败:', error)
    friends.value = []
  }
}

function selectFriend(friend) {
  inviteForm.selectedUser = friend
}

async function inviteFriend() {
  // TODO: 调用发送邀请 API
  ElMessage.success('邀请已发送')
  showInviteDialog.value = false
}

// ========== 分享链接 ==========
async function generateShareLink() {
  generatingLink.value = true
  try {
    const res = await createShareLink({
      documentId: route.params.id,
      permissionType: shareLinkForm.permissionType,
      validDays: shareLinkForm.validDays,
      maxUses: shareLinkForm.maxUses,
      verificationType: shareLinkForm.verificationType,
      password: shareLinkForm.password,
      email: shareLinkForm.email
    })
    
    if (res.code === 200) {
      generatedLink.value = window.location.origin + '/share/' + res.data.token
      ElMessage.success('链接生成成功')
    }
  } catch (error) {
    console.error('生成分享链接失败:', error)
    ElMessage.error('生成分享链接失败')
  } finally {
    generatingLink.value = false
  }
}

function copyLink() {
  navigator.clipboard.writeText(generatedLink.value)
  ElMessage.success('复制成功')
}

// ========== 工具函数 ==========
function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 删除文档
async function deleteDocument() {
  try {
    await ElMessageBox.confirm('确定要删除此文档吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteDocument(route.params.id)
    if (res.code === 200) {
      ElMessage.success('文档已删除')
      router.push('/home')
    }
  } catch {
    // 取消操作
  }
}
</script>

<style lang="scss" scoped>
.document-editor-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
}

// 顶部工具栏
.editor-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 70px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  backdrop-filter: blur(10px);
  
  .header-content {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
  }
  
  .header-left {
    flex: 1;
    min-width: 300px;
    
    .nav-group {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .back-btn {
        color: rgba(255, 255, 255, 0.9);
        font-size: 18px;
        transition: all 0.3s ease;
        
        &:hover {
          background: rgba(255, 255, 255, 0.2);
          transform: translateX(-2px);
        }
      }
      
      .title-wrapper {
        display: flex;
        align-items: center;
        gap: 12px;
        flex: 1;
        
        .doc-title-input {
          flex: 1;
          max-width: 400px;
          padding: 10px 16px;
          border: none;
          border-radius: 8px;
          background: rgba(255, 255, 255, 0.95);
          color: #1f2329;
          font-size: 15px;
          font-weight: 500;
          transition: all 0.3s ease;
          
          &::placeholder {
            color: rgba(0, 0, 0, 0.3);
          }
          
          &:focus {
            outline: none;
            background: white;
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
          }
        }
        
        .status-indicator {
          display: flex;
          gap: 8px;
          align-items: center;
        }
      }
    }
  }
  
  .header-center {
    flex: 0 0 auto;
    
    .online-users-wrapper {
      display: flex;
      align-items: center;
      gap: 12px;
      background: rgba(255, 255, 255, 0.15);
      padding: 8px 16px;
      border-radius: 12px;
      backdrop-filter: blur(10px);
      
      .users-label {
        display: flex;
        align-items: center;
        gap: 6px;
        color: white;
        font-size: 13px;
        font-weight: 500;
        
        .label-icon {
          font-size: 16px;
        }
        
        .count-tag {
          background: rgba(255, 255, 255, 0.3);
          border-color: transparent;
          color: white;
          font-weight: 600;
        }
      }
      
      .avatar-list {
        display: flex;
        gap: -8px;
        
        .online-avatar {
          border: 2px solid rgba(255, 255, 255, 0.5);
          cursor: pointer;
          transition: all 0.3s ease;
          
          &:hover {
            transform: scale(1.15);
            border-color: white;
          }
          
          &.self {
            border-color: #67c23a;
            background: linear-gradient(135deg, #a8e063 0%, #56ab2f 100%);
            color: white;
            font-weight: 600;
          }
        }
      }
    }
  }
  
  .header-right {
    flex: 0 0 auto;
    
    .action-buttons {
      display: flex;
      gap: 10px;
      align-items: center;
      
      .panel-btn {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        border-color: rgba(255, 255, 255, 0.3);
        backdrop-filter: blur(10px);
        transition: all 0.3s ease;
        
        &:hover {
          background: rgba(255, 255, 255, 0.3);
          transform: translateY(-2px);
        }
        
        .el-badge {
          margin-left: 4px;
        }
      }
      
      .save-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
        color: white;
        font-weight: 500;
        padding: 10px 24px;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
        }
      }
      
      .invite-btn {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        border: none;
        color: white;
        font-weight: 500;
        padding: 10px 24px;
        transition: all 0.3s ease;
        
        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 16px rgba(240, 147, 251, 0.4);
        }
      }
      
      .more-btn {
        background: rgba(255, 255, 255, 0.2);
        color: white;
        border-color: rgba(255, 255, 255, 0.3);
        transition: all 0.3s ease;
        
        &:hover {
          background: rgba(255, 255, 255, 0.3);
          transform: rotate(90deg);
        }
      }
    }
  }
}

// 编辑器主体
.editor-body {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  
  .editor-card {
    width: 100%;
    max-width: 1200px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    
    .toolbar-wrapper {
      border-bottom: 2px solid #f0f2f5;
      background: #fafafa;
      padding: 12px 20px;
      
      .quill-toolbar {
        :deep(.ql-toolbar) {
          border: none !important;
          background: transparent !important;
          padding: 0 !important;
          
          button {
            border-radius: 6px;
            transition: all 0.2s ease;
            
            &:hover {
              background: #e6e9ed;
              transform: scale(1.05);
            }
          }
          
          .ql-active {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-color: transparent;
          }
        }
      }
    }
    
    .editor-wrapper {
      flex: 1;
      overflow: hidden;
      
      .quill-editor {
        height: 100%;
        overflow-y: auto;
        
        :deep(.ql-editor) {
          border: none !important;
          padding: 40px 60px !important;
          font-size: 16px;
          line-height: 1.8;
          color: #1f2329;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
          
          &.ql-blank::before {
            color: rgba(0, 0, 0, 0.3);
            font-style: normal;
          }
        }
        
        :deep(.ql-container) {
          border: none !important;
        }
      }
    }
  }
}

// 悬浮面板
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-right-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.slide-right-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.floating-panel {
  position: fixed;
  right: 24px;
  top: 94px;
  width: 360px;
  max-height: calc(100vh - 118px);
  background: white;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 1000;
  overflow: hidden;
  
  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    flex-shrink: 0;
    
    .header-title {
      display: flex;
      align-items: center;
      gap: 10px;
      color: white;
      font-weight: 600;
      font-size: 16px;
      
      .title-icon {
        font-size: 18px;
      }
    }
    
    .close-btn {
      color: rgba(255, 255, 255, 0.8);
      transition: all 0.3s ease;
      
      &:hover {
        color: white;
        background: rgba(255, 255, 255, 0.2);
        transform: rotate(90deg);
      }
    }
  }
  
  .panel-content-wrapper {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    background: #f8f9fa;
  }
  
  .panel-content {
    padding: 16px;
    height: 100%;
    overflow-y: auto;
    background: #f8f9fa;
    
    // 评论面板
    &.comments-panel {
      .comment-input-wrapper {
        position: relative;
        
        .comment-input {
          :deep(.el-textarea__inner) {
            border-radius: 12px;
            border: 2px solid #e8eaed;
            background: white;
            padding: 12px 16px;
            font-size: 14px;
            transition: all 0.3s ease;
            resize: none;
            
            &:focus {
              border-color: #667eea;
              box-shadow: 0 2px 12px rgba(102, 126, 234, 0.1);
            }
          }
        }
        
        .comment-actions {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-top: 10px;
          
          .hint-text {
            font-size: 12px;
            color: #86909c;
          }
          
          .el-button {
            padding: 8px 20px;
            border-radius: 8px;
            font-weight: 500;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
            transition: all 0.3s ease;
            
            &:hover:not(:disabled) {
              transform: translateY(-2px);
              box-shadow: 0 6px 16px rgba(102, 126, 234, 0.3);
            }
            
            &:disabled {
              opacity: 0.5;
              cursor: not-allowed;
            }
          }
        }
      }
      
      .comment-list {
        max-height: calc(100vh - 380px);
        overflow-y: auto;
        
        .comment-item {
          display: flex;
          gap: 12px;
          padding: 14px;
          background: white;
          border-radius: 12px;
          margin-bottom: 12px;
          transition: all 0.3s ease;
          animation: slideInDown 0.3s ease;
          
          &:hover {
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
            transform: translateY(-2px);
          }
          
          .comment-avatar {
            flex-shrink: 0;
            border: 2px solid #e8eaed;
          }
          
          .comment-body {
            flex: 1;
            min-width: 0;
            
            .comment-header {
              display: flex;
              align-items: center;
              gap: 10px;
              margin-bottom: 6px;
              
              .comment-author {
                font-weight: 600;
                color: #1f2329;
                font-size: 14px;
              }
              
              .comment-time {
                font-size: 12px;
                color: #86909c;
                margin-left: auto;
              }
            }
            
            .comment-content {
              font-size: 14px;
              color: #4e5969;
              line-height: 1.6;
              word-wrap: break-word;
            }
            
            .comment-footer {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-top: 8px;
              
              .reply-btn,
              .like-btn,
              .delete-btn {
                height: auto;
                padding: 2px 6px;
                font-size: 12px;
                
                &:hover {
                  background: #f2f3f5;
                }
              }
              
              .like-btn {
                transition: all 0.3s ease;
                
                &.el-button--primary {
                  color: #409eff;
                  
                  &:hover {
                    color: #66b1ff;
                  }
                }
                
                &.el-button--danger {
                  color: #f56c6c;
                  font-weight: 600;
                  
                  &:hover {
                    color: #f78989;
                  }
                }
                
                &.liked {
                  animation: likeAnimation 0.3s ease;
                }
              }
            }
            
            .reply-input-wrapper {
              margin-top: 12px;
              animation: slideDown 0.3s ease;
              
              :deep(.el-textarea__inner) {
                border-radius: 8px;
                font-size: 13px;
              }
              
              .reply-actions {
                display: flex;
                justify-content: flex-end;
                gap: 8px;
                margin-top: 8px;
              }
            }
            
            .reply-list {
              margin-top: 12px;
              padding-left: 0;
              
              .reply-item {
                display: flex;
                gap: 10px;
                padding: 10px 0;
                
                .reply-avatar {
                  flex-shrink: 0;
                  cursor: pointer;
                  transition: transform 0.2s;
                  
                  &:hover {
                    transform: scale(1.1);
                  }
                }
                
                .reply-main {
                  flex: 1;
                  min-width: 0;
                  
                  .reply-header {
                    display: flex;
                    align-items: center;
                    gap: 6px;
                    margin-bottom: 4px;
                    
                    .reply-author {
                      font-weight: 600;
                      color: #1f2329;
                      font-size: 14px;
                    }
                    
                    .reply-arrow {
                      color: #86909c;
                      font-size: 12px;
                    }
                    
                    .reply-to-user {
                      font-size: 14px;
                      color: #86909c;
                    }
                  }
                  
                  .reply-text {
                    font-size: 14px;
                    color: #1f2329;
                    line-height: 1.6;
                    word-wrap: break-word;
                  }
                  
                  .reply-footer {
                    display: flex;
                    align-items: center;
                    gap: 16px;
                    margin-top: 6px;
                    
                    .reply-time {
                      font-size: 12px;
                      color: #86909c;
                    }
                    
                    .reply-btn {
                      height: auto;
                      padding: 0;
                      font-size: 12px;
                      color: #86909c;
                      
                      &:hover {
                        color: #409eff;
                        background: transparent;
                      }
                    }
                  }
                  
                  .reply-input-wrapper {
                    margin-top: 10px;
                    animation: slideDown 0.3s ease;
                    
                    :deep(.el-textarea__inner) {
                      border-radius: 8px;
                      font-size: 13px;
                    }
                    
                    .reply-actions {
                      margin-top: 8px;
                      display: flex;
                      justify-content: flex-end;
                      gap: 8px;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    
    // 成员面板
    &.members-panel {
      .panel-section {
        margin-bottom: 16px;
        
        .section-title {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 16px;
          font-size: 14px;
          font-weight: 600;
          color: #1f2329;
          
          .section-icon {
            color: #667eea;
            font-size: 16px;
          }
        }
        
        .user-list {
          .user-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px;
            background: white;
            border-radius: 10px;
            margin-bottom: 10px;
            transition: all 0.3s ease;
            
            &:hover {
              box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
              transform: translateX(4px);
            }
            
            .user-avatar {
              border: 2px solid #e8eaed;
              margin-right: 12px;
            }
            
            .user-info {
              flex: 1;
              min-width: 0;
              
              .user-name {
                font-weight: 600;
                color: #1f2329;
                font-size: 14px;
                margin-bottom: 4px;
              }
              
              .user-status,
              .user-permission {
                font-size: 12px;
                color: #86909c;
              }
            }
            
            .permission-select {
              width: 100px;
              margin-left: 12px;
            }
          }
        }
      }
    }
  }
}

// 动画效果
.comment-list-enter-active,
.comment-list-leave-active {
  transition: all 0.3s ease;
}

.comment-list-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.comment-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes likeAnimation {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.3);
  }
  100% {
    transform: scale(1);
  }
}

// 邀请对话框
:deep(.invite-dialog) {
  .el-dialog__header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 20px 24px;
    border-radius: 12px 12px 0 0;
    
    .el-dialog__title {
      color: white;
      font-weight: 600;
      font-size: 18px;
    }
    
    .el-dialog__headerbtn {
      :deep(.el-dialog__close) {
        color: rgba(255, 255, 255, 0.8);
        
        &:hover {
          color: white;
        }
      }
    }
  }
  
  .el-tabs__header {
    padding: 0 24px;
    margin-top: 16px;
  }
}

.invite-section {
  padding: 8px 0;
  
  .friend-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
    max-height: 400px;
    overflow-y: auto;
    padding: 8px;
    
    .friend-card {
      cursor: pointer;
      transition: all 0.3s ease;
      border-radius: 12px;
      border: 2px solid transparent;
      
      &:hover {
        border-color: #667eea;
        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.2);
        transform: translateY(-4px);
      }
      
      &.selected {
        border-color: #667eea;
        background: linear-gradient(135deg, rgba(102, 126, 234, 0.05) 0%, rgba(118, 75, 162, 0.05) 100%);
        box-shadow: 0 4px 16px rgba(102, 126, 234, 0.15);
      }
      
      .friend-card-content {
        display: flex;
        align-items: center;
        gap: 12px;
        position: relative;
        padding: 12px;
        
        .friend-card-info {
          flex: 1;
          min-width: 0;
          
          .friend-name {
            font-weight: 600;
            color: #1f2329;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 14px;
          }
          
          .friend-username {
            font-size: 12px;
            color: #86909c;
            margin-top: 4px;
          }
        }
        
        .check-icon {
          color: #67c23a;
          font-size: 20px;
          flex-shrink: 0;
        }
      }
    }
  }
}
</style>
