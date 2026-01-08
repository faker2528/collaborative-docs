<template>
  <div class="friends-container">
    <el-row :gutter="20">
      <!-- 左侧：好友列表 -->
      <el-col :span="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的好友 ({{ friends.length }})</span>
              <el-button type="primary" size="small" @click="showAddDialog = true">
                <el-icon><Plus /></el-icon>
                添加好友
              </el-button>
            </div>
          </template>
          
          <div class="search-bar">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索好友"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          
          <el-empty v-if="filteredFriends.length === 0" description="暂无好友" />
          
          <div v-else class="friend-list">
            <div 
              v-for="friend in filteredFriends" 
              :key="friend.id" 
              class="friend-item"
            >
              <el-avatar :size="48" :src="friend.avatar">
                {{ friend.nickname?.charAt(0) || friend.username?.charAt(0) }}
              </el-avatar>
              <div class="friend-info">
                <div class="friend-name">{{ friend.nickname || friend.username }}</div>
                <div class="friend-username">@{{ friend.username }}</div>
              </div>
              <div class="friend-actions">
                <el-button size="small" type="danger" text @click="handleDeleteFriend(friend)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：好友请求 -->
      <el-col :span="10">
        <el-card>
          <template #header>
            <span>好友请求</span>
            <el-badge v-if="pendingRequests.length > 0" :value="pendingRequests.length" class="badge" />
          </template>
          
          <el-tabs v-model="activeTab">
            <el-tab-pane label="收到的请求" name="received">
              <el-empty v-if="receivedRequests.length === 0" description="暂无请求" />
              <div v-else class="request-list">
                <div 
                  v-for="req in receivedRequests" 
                  :key="req.id" 
                  class="request-item"
                >
                  <el-avatar :size="40" :src="req.fromAvatar">
                    {{ req.fromNickname?.charAt(0) || req.fromUsername?.charAt(0) }}
                  </el-avatar>
                  <div class="request-info">
                    <div class="request-name">{{ req.fromNickname || req.fromUsername }}</div>
                    <div class="request-message" v-if="req.message">{{ req.message }}</div>
                    <div class="request-time">{{ formatTime(req.createTime) }}</div>
                  </div>
                  <div class="request-actions" v-if="req.status === 0">
                    <el-button size="small" type="primary" @click="handleAccept(req)">同意</el-button>
                    <el-button size="small" @click="handleReject(req)">拒绝</el-button>
                  </div>
                  <el-tag v-else :type="req.status === 1 ? 'success' : 'info'" size="small">
                    {{ req.status === 1 ? '已同意' : '已拒绝' }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="发出的请求" name="sent">
              <el-empty v-if="sentRequests.length === 0" description="暂无请求" />
              <div v-else class="request-list">
                <div 
                  v-for="req in sentRequests" 
                  :key="req.id" 
                  class="request-item"
                >
                  <div class="request-info">
                    <div class="request-name">用户ID: {{ req.toUserId }}</div>
                    <div class="request-time">{{ formatTime(req.createTime) }}</div>
                  </div>
                  <el-tag :type="getStatusType(req.status)" size="small">
                    {{ getStatusText(req.status) }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 添加好友对话框 -->
    <el-dialog v-model="showAddDialog" title="添加好友" width="500px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="搜索用户">
          <el-input 
            v-model="addForm.keyword" 
            placeholder="输入用户名搜索"
            @input="handleSearchUsers"
            clearable
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <div class="search-results" v-if="searchResults.length > 0">
          <div 
            v-for="user in searchResults" 
            :key="user.id" 
            class="user-item"
            :class="{ selected: addForm.selectedUser?.id === user.id }"
            @click="addForm.selectedUser = user"
          >
            <el-avatar :size="36">{{ user.username.charAt(0) }}</el-avatar>
            <div class="user-info">
              <span class="username">{{ user.username }}</span>
              <span class="nickname" v-if="user.nickname">({{ user.nickname }})</span>
            </div>
            <el-icon v-if="addForm.selectedUser?.id === user.id" class="check-icon"><Check /></el-icon>
          </div>
        </div>
        <div class="no-results" v-else-if="addForm.keyword && !searchLoading">
          未找到用户
        </div>
        
        <el-form-item label="附加消息" v-if="addForm.selectedUser">
          <el-input 
            v-model="addForm.message" 
            type="textarea" 
            placeholder="可选：输入验证消息"
            :rows="2"
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSendRequest" :disabled="!addForm.selectedUser">
          发送请求
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getFriendList, 
  getReceivedRequests, 
  getSentRequests,
  acceptFriendRequest,
  rejectFriendRequest,
  deleteFriend,
  sendFriendRequest 
} from '@/api/friend'
import { searchUsers } from '@/api/user'

const friends = ref([])
const receivedRequests = ref([])
const sentRequests = ref([])
const searchKeyword = ref('')
const activeTab = ref('received')
const showAddDialog = ref(false)
const searchLoading = ref(false)
const searchResults = ref([])
let searchTimer = null

const addForm = reactive({
  keyword: '',
  selectedUser: null,
  message: ''
})

const filteredFriends = computed(() => {
  if (!searchKeyword.value) return friends.value
  const keyword = searchKeyword.value.toLowerCase()
  return friends.value.filter(f => 
    f.username?.toLowerCase().includes(keyword) ||
    f.nickname?.toLowerCase().includes(keyword)
  )
})

const pendingRequests = computed(() => {
  return receivedRequests.value.filter(r => r.status === 0)
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  await Promise.all([
    fetchFriends(),
    fetchReceivedRequests(),
    fetchSentRequests()
  ])
}

async function fetchFriends() {
  try {
    const res = await getFriendList()
    if (res.code === 200) {
      friends.value = res.data || []
    }
  } catch (e) {
    console.error('获取好友列表失败')
  }
}

async function fetchReceivedRequests() {
  try {
    const res = await getReceivedRequests()
    if (res.code === 200) {
      receivedRequests.value = res.data || []
    }
  } catch (e) {
    console.error('获取收到的请求失败')
  }
}

async function fetchSentRequests() {
  try {
    const res = await getSentRequests()
    if (res.code === 200) {
      sentRequests.value = res.data || []
    }
  } catch (e) {
    console.error('获取发出的请求失败')
  }
}

async function handleAccept(req) {
  try {
    const res = await acceptFriendRequest(req.id)
    if (res.code === 200) {
      ElMessage.success('已添加好友')
      fetchData()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleReject(req) {
  try {
    const res = await rejectFriendRequest(req.id)
    if (res.code === 200) {
      ElMessage.success('已拒绝请求')
      fetchReceivedRequests()
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function handleDeleteFriend(friend) {
  try {
    await ElMessageBox.confirm(`确定要删除好友"${friend.nickname || friend.username}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteFriend(friend.id)
    if (res.code === 200) {
      ElMessage.success('已删除好友')
      fetchFriends()
    }
  } catch (e) {
    // 取消或失败
  }
}

function handleSearchUsers() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  
  if (!addForm.keyword.trim()) {
    searchResults.value = []
    return
  }
  
  searchTimer = setTimeout(async () => {
    searchLoading.value = true
    try {
      const res = await searchUsers(addForm.keyword)
      if (res.code === 200) {
        searchResults.value = res.data || []
      }
    } catch (e) {
      console.error('搜索用户失败')
    } finally {
      searchLoading.value = false
    }
  }, 300)
}

async function handleSendRequest() {
  if (!addForm.selectedUser) return
  
  try {
    const res = await sendFriendRequest(addForm.selectedUser.id, addForm.message)
    if (res.code === 200) {
      ElMessage.success('好友请求已发送')
      showAddDialog.value = false
      addForm.keyword = ''
      addForm.selectedUser = null
      addForm.message = ''
      searchResults.value = []
      fetchSentRequests()
    }
  } catch (e) {
    ElMessage.error('发送失败')
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

function getStatusType(status) {
  const types = { 0: 'warning', 1: 'success', 2: 'info' }
  return types[status] || 'info'
}

function getStatusText(status) {
  const texts = { 0: '待处理', 1: '已同意', 2: '已拒绝' }
  return texts[status] || '未知'
}
</script>

<style scoped lang="scss">
.friends-container {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .badge {
    margin-left: 8px;
  }
}

.search-bar {
  margin-bottom: 16px;
}

.friend-list {
  .friend-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    transition: background-color 0.2s;
    
    &:hover {
      background-color: #f5f7fa;
    }
    
    .friend-info {
      flex: 1;
      margin-left: 12px;
      
      .friend-name {
        font-weight: 500;
        color: #303133;
      }
      
      .friend-username {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.request-list {
  .request-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #ebeef5;
    
    &:last-child {
      border-bottom: none;
    }
    
    .request-info {
      flex: 1;
      margin-left: 12px;
      
      .request-name {
        font-weight: 500;
        color: #303133;
      }
      
      .request-message {
        font-size: 12px;
        color: #606266;
        margin-top: 4px;
      }
      
      .request-time {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
    
    .request-actions {
      display: flex;
      gap: 8px;
    }
  }
}

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
      }
      
      .nickname {
        color: #909399;
        font-size: 12px;
        margin-left: 4px;
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
}
</style>
