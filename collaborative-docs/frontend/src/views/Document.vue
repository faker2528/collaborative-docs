<template>
  <div class="document-container">
    <!-- 文档头部 -->
    <div class="document-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <h3 class="document-title">{{ documentStore.currentDocument?.title }}</h3>
        <el-tag v-if="connectionStatus === 'connected'" type="success" size="small">已连接</el-tag>
        <el-tag v-else-if="connectionStatus === 'connecting'" type="warning" size="small">连接中...</el-tag>
        <el-tag v-else type="danger" size="small">未连接</el-tag>
      </div>
      
      <div class="header-right">
        <!-- 在线用户 -->
        <div class="online-users">
          <span class="label">在线:</span>
          <div class="avatar-list">
            <div 
              v-for="user in onlineUsers" 
              :key="user.siteId"
              class="avatar-wrapper"
              @click="goToUserProfile(user.userId)"
            >
              <el-tooltip :content="user.username" placement="bottom">
                <el-avatar 
                  :size="28"
                  class="clickable-avatar"
                >{{ user.username?.charAt(0) }}</el-avatar>
              </el-tooltip>
            </div>
          </div>
        </div>
        
        <el-button type="primary" size="small" @click="saveDocument">
          <el-icon><DocumentChecked /></el-icon>
          保存
        </el-button>
        
        <el-button type="success" size="small" @click="showInviteDialog = true">
          <el-icon><Share /></el-icon>
          邀请协作
        </el-button>
        
        <el-button type="warning" size="small" @click="openManageDialog">
          <el-icon><Setting /></el-icon>
          管理
        </el-button>
      </div>
    </div>
    
    <!-- Quill编辑器 -->
    <div class="editor-container">
      <div ref="editorRef" class="quill-editor"></div>
    </div>
    
    <!-- 邀请协作对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请协作" width="550px">
      <el-tabs v-model="inviteTab">
        <!-- 好友邀请 -->
        <el-tab-pane label="邀请好友" name="friend">
          <div class="invite-dialog-content">
            <el-empty v-if="friends.length === 0" description="暂无好友，请先添加好友">
              <el-button type="primary" @click="$router.push('/friends')">去添加好友</el-button>
            </el-empty>
            
            <div v-else class="friend-select-list">
              <div 
                v-for="friend in friends" 
                :key="friend.id" 
                class="user-item"
                :class="{ selected: inviteForm.selectedUser?.id === friend.id }"
                @click="selectUser(friend)"
              >
                <el-avatar :size="32">{{ friend.username.charAt(0) }}</el-avatar>
                <div class="user-info">
                  <span class="username">{{ friend.nickname || friend.username }}</span>
                  <span class="nickname">@{{ friend.username }}</span>
                </div>
                <el-icon v-if="inviteForm.selectedUser?.id === friend.id" class="check-icon"><Check /></el-icon>
              </div>
            </div>
            
            <el-form-item label="权限类型" v-if="inviteForm.selectedUser && isDocumentCreator" style="margin-top: 16px;">
              <el-radio-group v-model="inviteForm.permissionType">
                <el-radio :value="1">只读</el-radio>
                <el-radio :value="2">可编辑</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-alert 
              v-if="inviteForm.selectedUser && !isDocumentCreator" 
              type="info" 
              :closable="false"
              style="margin-top: 16px;"
            >
              您不是文档创建者，邀请的用户将以只读权限加入
            </el-alert>
            
            <div class="selected-user-tip" v-if="inviteForm.selectedUser">
              已选择: <strong>{{ inviteForm.selectedUser.nickname || inviteForm.selectedUser.username }}</strong>
            </div>
          </div>
          
          <div class="dialog-footer">
            <el-button @click="showInviteDialog = false">取消</el-button>
            <el-button type="primary" @click="inviteUser" :disabled="!inviteForm.selectedUser">
              确认邀请
            </el-button>
          </div>
        </el-tab-pane>
        
        <!-- 链接分享 -->
        <el-tab-pane label="生成分享链接" name="link">
          <div class="invite-dialog-content">
            <el-form :model="shareLinkForm" label-width="100px">
              <el-form-item label="权限类型" v-if="isDocumentCreator">
                <el-radio-group v-model="shareLinkForm.permissionType">
                  <el-radio :value="1">只读</el-radio>
                  <el-radio :value="2">可编辑</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-alert 
                v-if="!isDocumentCreator" 
                type="info" 
                :closable="false"
                style="margin-bottom: 16px;"
              >
                您不是文档创建者，生成的链接只能为只读权限
              </el-alert>
              
              <el-form-item label="有效期">
                <el-select v-model="shareLinkForm.validDays" style="width: 100%;">
                  <el-option :value="0" label="永久有效" />
                  <el-option :value="1" label="1天" />
                  <el-option :value="7" label="7天" />
                  <el-option :value="30" label="30天" />
                </el-select>
              </el-form-item>
              
              <el-form-item label="使用次数限制">
                <el-select v-model="shareLinkForm.maxUses" style="width: 100%;">
                  <el-option :value="0" label="不限制" />
                  <el-option :value="1" label="1次" />
                  <el-option :value="5" label="5次" />
                  <el-option :value="10" label="10次" />
                </el-select>
              </el-form-item>
            </el-form>
            
            <!-- 生成的链接 -->
            <div class="share-link-result" v-if="generatedLink">
              <el-input v-model="generatedLink" readonly>
                <template #append>
                  <el-button @click="copyLink">
                    <el-icon><CopyDocument /></el-icon>
                    复制
                  </el-button>
                </template>
              </el-input>
              <p class="link-tip">请将链接发送给需要协作的用户</p>
            </div>
          </div>
          
          <div class="dialog-footer">
            <el-button @click="showInviteDialog = false">关闭</el-button>
            <el-button type="primary" @click="generateShareLink" :loading="generatingLink">
              {{ generatedLink ? '重新生成' : '生成链接' }}
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
    <!-- 文档管理对话框 -->
    <el-dialog v-model="showManageDialog" title="文档管理" width="550px">
      <div class="manage-dialog-content">
        <h4 style="margin: 0 0 16px;">文档成员 ({{ documentMembers.length }})</h4>
        
        <el-empty v-if="documentMembers.length === 0" description="暂无成员"></el-empty>
        
        <div v-else class="member-list">
          <div v-for="member in documentMembers" :key="member.userId" class="member-item">
            <el-avatar 
              :size="36" 
              class="clickable-avatar"
              @click.stop="goToUserProfile(member.userId)"
            >{{ member.username?.charAt(0) }}</el-avatar>
            <div class="member-info">
              <span class="username">{{ member.nickname || member.username }}</span>
              <el-tag v-if="member.isCreator" type="warning" size="small">创建者</el-tag>
            </div>
            
            <div class="member-actions">
              <el-select 
                v-if="!member.isCreator && isDocumentCreator" 
                v-model="member.permissionType" 
                size="small"
                style="width: 100px;"
                @change="handlePermissionChange(member)"
              >
                <el-option :value="1" label="只读" />
                <el-option :value="2" label="可编辑" />
              </el-select>
              
              <el-tag v-if="!member.isCreator && !isDocumentCreator" size="small">
                {{ member.permissionType === 2 ? '可编辑' : '只读' }}
              </el-tag>
              
              <el-button 
                v-if="!member.isCreator && isDocumentCreator" 
                type="danger" 
                size="small"
                text
                @click="handleRemoveMember(member)"
              >
                移除
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showManageDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Quill from 'quill'
import 'quill/dist/quill.snow.css'
import { useDocumentStore } from '@/stores/document'
import { useUserStore } from '@/stores/user'
import { updateDocument, shareDocument, createShareLink, getDocumentMembers, updateMemberPermission, removeMember } from '@/api/document'
import { getFriendList } from '@/api/friend'

const route = useRoute()
const router = useRouter()
const documentStore = useDocumentStore()
const userStore = useUserStore()

// 判断当前用户是否是文档创建者
const isDocumentCreator = computed(() => {
  const doc = documentStore.currentDocument
  const currentUserId = userStore.userInfo?.userId || userStore.userInfo?.id
  return doc && currentUserId && String(doc.creatorId) === String(currentUserId)
})

const editorRef = ref(null)
const connectionStatus = ref('disconnected')
const onlineUsers = ref([])

// 邀请功能相关
const showInviteDialog = ref(false)
const inviteTab = ref('friend')
const friends = ref([])
const inviteForm = reactive({
  selectedUser: null,
  permissionType: 2  // 默认可编辑
})

// 分享链接相关
const shareLinkForm = reactive({
  permissionType: 2,
  validDays: 7,
  maxUses: 0
})
const generatedLink = ref('')
const generatingLink = ref(false)

// 文档管理相关
const showManageDialog = ref(false)
const documentMembers = ref([])

let quill = null
let ws = null
let siteId = null
let isRemoteChange = false
let isComposing = false // 输入法组合状态
let pendingDelta = null // 组合过程中累积的 Delta

// 获取文档ID
const documentId = String(route.params.id)

onMounted(async () => {
  // 首先尝试从store中获取文档（如果刚创建）
  const existingDoc = documentStore.documents.find(doc => String(doc.id) === documentId)
  if (existingDoc) {
    documentStore.currentDocument = existingDoc
  }
  
  // 加载文档，最多重试3次
  let attempts = 0
  const maxAttempts = 3
  
  // 直接使用字符串ID，避免JavaScript Number精度问题
  // 后端会自动将字符串转换为Long类型
  
  while (attempts < maxAttempts) {
    const res = await documentStore.fetchDocument(documentId)  // 传入字符串ID
    if (res.code === 200) {
      break
    }
    
    attempts++
    if (attempts < maxAttempts) {
      console.log(`获取文档失败，正在重试 (${attempts}/${maxAttempts})`)
      await new Promise(resolve => setTimeout(resolve, 500))
    } else {
      ElMessage.error('文档加载失败')
      router.push('/')
      return
    }
  }
  
  await nextTick()
  
  // 初始化Quill编辑器
  initQuill()
  
  // 连接WebSocket
  connectWebSocket()
})

onUnmounted(() => {
  // 关闭WebSocket连接
  if (ws) {
    ws.close()
    ws = null
  }
})

function initQuill() {
  quill = new Quill(editorRef.value, {
    theme: 'snow',
    placeholder: '开始输入内容...',
    modules: {
      toolbar: [
        [{ header: [1, 2, 3, false] }],
        ['bold', 'italic', 'underline', 'strike'],
        [{ color: [] }, { background: [] }],
        [{ list: 'ordered' }, { list: 'bullet' }],
        [{ indent: '-1' }, { indent: '+1' }],
        [{ align: [] }],
        ['link', 'image'],
        ['clean']
      ]
    }
  })
  
  // 设置初始内容
  if (documentStore.currentDocument?.content) {
    try {
      const delta = JSON.parse(documentStore.currentDocument.content)
      quill.setContents(delta)
    } catch {
      quill.setText(documentStore.currentDocument.content)
    }
  }
  
  // 监听文本变化
  quill.on('text-change', handleTextChange)
  
  // 监听输入法组合事件（处理中文输入）
  const editorElement = editorRef.value.querySelector('.ql-editor')
  if (editorElement) {
    editorElement.addEventListener('compositionstart', () => {
      isComposing = true
      pendingDelta = null
    })
    editorElement.addEventListener('compositionend', () => {
      isComposing = false
      // 组合结束后，发送累积的 Delta
      if (pendingDelta) {
        sendDelta(pendingDelta)
        pendingDelta = null
      }
    })
  }
}

function handleTextChange(delta, oldDelta, source) {
  if (source !== 'user' || isRemoteChange) return
  
  // 在输入法组合过程中，累积 Delta，等组合结束后再发送
  if (isComposing) {
    // 累积 Delta
    if (pendingDelta) {
      pendingDelta = pendingDelta.compose(delta)
    } else {
      pendingDelta = delta
    }
    return
  }
  
  // 直接发送
  sendDelta(delta)
}

function sendDelta(delta) {
  if (!delta || !ws || ws.readyState !== WebSocket.OPEN) return
  
  const message = {
    type: 'OPERATION',
    documentId: parseInt(documentId),
    operation: {
      type: 'INSERT',
      siteId: siteId,
      clock: Date.now(),
      documentId: parseInt(documentId),
      timestamp: Date.now(),
      // 简化的Delta格式传输
      character: {
        id: { siteId: siteId, clock: Date.now() },
        value: JSON.stringify(delta),
        deleted: false,
        prevId: null
      }
    }
  }
  ws.send(JSON.stringify(message))
}

function connectWebSocket() {
  const token = userStore.token
  const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:8080'
  const wsUrl = `${wsBaseUrl}/ws/collaboration/${documentId}?token=${token}`
  
  connectionStatus.value = 'connecting'
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    console.log('WebSocket connected')
    connectionStatus.value = 'connected'
  }
  
  ws.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)
      handleWebSocketMessage(message)
    } catch (e) {
      console.error('Failed to parse WebSocket message:', e)
    }
  }
  
  ws.onclose = () => {
    console.log('WebSocket disconnected')
    connectionStatus.value = 'disconnected'
    
    // 尝试重连
    setTimeout(() => {
      if (connectionStatus.value === 'disconnected') {
        connectWebSocket()
      }
    }, 3000)
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
    connectionStatus.value = 'disconnected'
  }
}

function handleWebSocketMessage(message) {
  console.log('Received message:', message.type)
  
  switch (message.type) {
    case 'JOINED':
      // 加入成功，保存siteId
      siteId = message.siteId
      // 使用扩展运算符创建新数组，确保 Vue 检测到变化
      onlineUsers.value = [...(message.onlineUsers || [])]
      
      // 如果服务端有内容，同步到编辑器
      if (message.content && quill) {
        isRemoteChange = true
        try {
          // 服务端返回的是 Delta JSON 格式
          const delta = JSON.parse(message.content)
          quill.setContents(delta)
        } catch {
          // 如果解析失败，尝试作为纯文本处理
          quill.setText(message.content)
        }
        isRemoteChange = false
      }
      break
      
    case 'USER_JOINED':
      // 其他用户加入
      onlineUsers.value = [...(message.onlineUsers || [])]
      ElMessage.info(`${message.username} 加入了文档`)
      break
      
    case 'USER_LEFT':
      // 其他用户离开
      onlineUsers.value = [...(message.onlineUsers || [])]
      ElMessage.info(`${message.username} 离开了文档`)
      break
      
    case 'REMOTE_OPERATION':
      // 应用远程操作
      applyRemoteOperation(message.operation)
      break
      
    case 'REMOTE_OPERATIONS':
      // 应用批量远程操作
      if (message.operations) {
        message.operations.forEach(op => applyRemoteOperation(op))
      }
      break
      
    case 'SYNC_RESPONSE':
      // 同步响应
      if (message.content && quill) {
        isRemoteChange = true
        try {
          const delta = JSON.parse(message.content)
          quill.setContents(delta)
        } catch {
          quill.setText(message.content)
        }
        isRemoteChange = false
      }
      break
      
    case 'ERROR':
      ElMessage.error(message.error || '协作服务错误')
      break
  }
}

function applyRemoteOperation(operation) {
  if (!operation || !quill) return
  
  try {
    isRemoteChange = true
    
    // 解析并应用Delta
    if (operation.character && operation.character.value) {
      const delta = JSON.parse(operation.character.value)
      quill.updateContents(delta)
    }
  } catch (e) {
    console.error('Failed to apply remote operation:', e)
  } finally {
    isRemoteChange = false
  }
}

async function saveDocument() {
  if (!quill) return
  
  const content = JSON.stringify(quill.getContents())
  
  try {
    const res = await updateDocument(documentId, content)
    if (res.code === 200) {
      ElMessage.success('文档保存成功')
    }
  } catch (e) {
    ElMessage.error('文档保存失败')
  }
}

function goBack() {
  router.push('/')
}

// 跳转到用户主页
function goToUserProfile(userId) {
  console.log('Document goToUserProfile called with userId:', userId)
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

// 获取好友列表
async function fetchFriends() {
  try {
    const res = await getFriendList()
    if (res.code === 200) {
      friends.value = res.data || []
    }
  } catch (e) {
    console.error('获取好友列表失败:', e)
  }
}

// 选择用户
function selectUser(user) {
  inviteForm.selectedUser = user
}

// 邀请好友协作
async function inviteUser() {
  if (!inviteForm.selectedUser) {
    ElMessage.warning('请先选择要邀请的好友')
    return
  }
  
  try {
    const res = await shareDocument(
      documentId, 
      inviteForm.selectedUser.id, 
      inviteForm.permissionType
    )
    if (res.code === 200) {
      ElMessage.success(`已成功邀请 ${inviteForm.selectedUser.nickname || inviteForm.selectedUser.username} 协作编辑文档`)
      // 重置表单
      showInviteDialog.value = false
      inviteForm.selectedUser = null
      inviteForm.permissionType = 2
    }
  } catch (e) {
    ElMessage.error('邀请失败，请重试')
  }
}

// 生成分享链接
async function generateShareLink() {
  generatingLink.value = true
  try {
    // 非创建者只能生成只读链接
    const permissionType = isDocumentCreator.value ? shareLinkForm.permissionType : 1
    
    const res = await createShareLink({
      documentId: documentId,
      permissionType: permissionType,
      validDays: shareLinkForm.validDays,
      maxUses: shareLinkForm.maxUses
    })
    if (res.code === 200) {
      const baseUrl = window.location.origin
      generatedLink.value = `${baseUrl}/share/${res.data.token}`
      ElMessage.success('分享链接生成成功')
    }
  } catch (e) {
    ElMessage.error('生成链接失败，请重试')
  } finally {
    generatingLink.value = false
  }
}

// 复制链接
function copyLink() {
  navigator.clipboard.writeText(generatedLink.value).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败，请手动复制')
  })
}

// 打开邀请对话框时获取好友列表
import { watch } from 'vue'
watch(showInviteDialog, (newVal) => {
  if (newVal) {
    fetchFriends()
    generatedLink.value = ''
  }
})

// 打开文档管理对话框
async function openManageDialog() {
  showManageDialog.value = true
  await fetchDocumentMembers()
}

// 获取文档成员列表
async function fetchDocumentMembers() {
  try {
    const res = await getDocumentMembers(documentId)
    if (res.code === 200) {
      documentMembers.value = res.data || []
    }
  } catch (e) {
    console.error('获取文档成员失败:', e)
    ElMessage.error('获取文档成员失败')
  }
}

// 修改成员权限
async function handlePermissionChange(member) {
  try {
    const res = await updateMemberPermission(documentId, member.userId, member.permissionType)
    if (res.code === 200) {
      ElMessage.success('权限修改成功')
    }
  } catch (e) {
    ElMessage.error('权限修改失败')
    // 回滚
    await fetchDocumentMembers()
  }
}

// 移除文档成员
async function handleRemoveMember(member) {
  try {
    await ElMessageBox.confirm(
      `确定要移除成员 "${member.nickname || member.username}" 吗？`,
      '确认移除',
      { type: 'warning' }
    )
    
    const res = await removeMember(documentId, member.userId)
    if (res.code === 200) {
      ElMessage.success('成员已移除')
      await fetchDocumentMembers()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('移除成员失败')
    }
  }
}
</script>

<style scoped lang="scss">
.document-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .document-title {
      margin: 0;
      font-size: 16px;
      color: #303133;
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .online-users {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .label {
        color: #909399;
        font-size: 14px;
      }
      
      .avatar-list {
        display: flex;
        align-items: center;
        gap: -8px;
        
        .avatar-wrapper {
          cursor: pointer;
          
          &:hover .clickable-avatar {
            transform: scale(1.1);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          }
        }
      }
      
      .clickable-avatar {
        cursor: pointer;
        transition: transform 0.2s, box-shadow 0.2s;
        border: 2px solid #fff;
      }
    }
  }
}

.editor-container {
  flex: 1;
  overflow: hidden;
  
  .quill-editor {
    height: 100%;
    
    :deep(.ql-container) {
      height: calc(100% - 42px);
      font-size: 16px;
    }
    
    :deep(.ql-editor) {
      min-height: 100%;
      padding: 20px 40px;
    }
  }
}

// 邀请对话框样式
.invite-dialog-content {
  .friend-select-list,
  .search-results {
    max-height: 200px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    margin-bottom: 16px;
    
    .user-item {
      display: flex;
      align-items: center;
      padding: 10px 12px;
      cursor: pointer;
      transition: background-color 0.2s;
      
      &:hover {
        background-color: #f5f7fa;
      }
      
      &.selected {
        background-color: #ecf5ff;
        border-left: 3px solid #409eff;
      }
      
      .user-info {
        margin-left: 12px;
        flex: 1;
        
        .username {
          font-weight: 500;
          color: #303133;
          display: block;
        }
        
        .nickname {
          color: #909399;
          font-size: 12px;
          display: block;
        }
      }
      
      .check-icon {
        color: #409eff;
        font-size: 16px;
      }
    }
  }
  
  .no-results {
    text-align: center;
    padding: 20px;
    color: #909399;
    font-size: 14px;
  }
  
  .selected-user-tip {
    background-color: #f0f9eb;
    padding: 10px 12px;
    border-radius: 4px;
    color: #67c23a;
    font-size: 14px;
    margin-top: 12px;
    
    strong {
      color: #409eff;
    }
  }
  
  .share-link-result {
    margin-top: 16px;
    
    .link-tip {
      margin: 12px 0 0;
      font-size: 12px;
      color: #909399;
      text-align: center;
    }
  }
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}

// 文档管理对话框样式
.manage-dialog-content {
  .member-list {
    max-height: 350px;
    overflow-y: auto;
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    
    .member-item {
      display: flex;
      align-items: center;
      padding: 12px;
      border-bottom: 1px solid #ebeef5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .member-info {
        flex: 1;
        margin-left: 12px;
        display: flex;
        align-items: center;
        gap: 8px;
        
        .username {
          font-weight: 500;
          color: #303133;
        }
      }
      
      .member-actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }
  }
}
</style>
