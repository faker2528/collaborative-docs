<template>
  <div class="friends-container">
    <el-row :gutter="24">
      <!-- 左侧：好友列表 -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="hover" class="friends-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <h2 class="page-title">我的好友</h2>
                <el-tag type="primary" round>{{ filteredFriends.length }}</el-tag>
              </div>
              <el-button type="primary" @click="showAddDialog = true">
                <el-icon><Plus /></el-icon>
                添加好友
              </el-button>
            </div>
          </template>
          
          <!-- 搜索栏 -->
          <div class="search-bar">
            <el-input 
              v-model="searchKeyword" 
              placeholder="搜索好友昵称或用户名"
              clearable
              size="large"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          
          <!-- 好友列表 -->
          <div v-loading="loading" class="friend-list-wrapper">
            <el-empty v-if="!loading && filteredFriends.length === 0" description="暂无好友">
              <el-button type="primary" @click="showAddDialog = true">去添加好友</el-button>
            </el-empty>
            
            <div v-else class="friend-list">
              <el-card 
                v-for="friend in filteredFriends" 
                :key="friend.id" 
                shadow="hover"
                class="friend-item"
              >
                <div class="friend-content">
                  <div class="friend-left">
                    <el-avatar 
                      :size="56" 
                      :src="friend.avatar"
                      class="friend-avatar"
                      @click="goToUserProfile(friend.id)"
                    >
                      {{ friend.nickname?.charAt(0) || friend.username?.charAt(0) }}
                    </el-avatar>
                    <div class="friend-info">
                      <div class="friend-name">{{ friend.nickname || friend.username }}</div>
                      <div class="friend-username">@{{ friend.username }}</div>
                      <div class="friend-bio" v-if="friend.bio">{{ friend.bio }}</div>
                    </div>
                  </div>
                  <div class="friend-actions">
                    <el-button 
                      type="primary" 
                      text 
                      @click="startChat(friend.id)"
                    >
                      <el-icon><ChatDotRound /></el-icon>
                      发消息
                    </el-button>
                    <el-dropdown trigger="click" @command="(cmd) => handleFriendCommand(cmd, friend)">
                      <el-button text>
                        <el-icon><MoreFilled /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item command="view">
                            <el-icon><User /></el-icon>
                            查看主页
                          </el-dropdown-item>
                          <el-dropdown-item command="delete" divided>
                            <el-icon><Delete /></el-icon>
                            删除好友
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </div>
              </el-card>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <!-- 右侧：好友请求 -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="hover" class="requests-card">
          <template #header>
            <div class="request-header">
              <h3 class="request-title">好友请求</h3>
              <el-badge 
                v-if="pendingRequests.length > 0" 
                :value="pendingRequests.length" 
                :max="99"
              />
            </div>
          </template>
          
          <el-tabs v-model="activeTab">
            <el-tab-pane label="收到的请求" name="received">
              <div v-loading="loading" class="request-list-wrapper">
                <el-empty v-if="!loading && receivedRequests.length === 0" description="暂无新请求" />
                <div v-else class="request-list">
                  <el-card 
                    v-for="req in receivedRequests" 
                    :key="req.id" 
                    shadow="hover"
                    class="request-item"
                  >
                    <div class="request-content">
                      <el-avatar 
                        :size="48" 
                        :src="req.fromAvatar"
                        @click="goToUserProfile(req.senderId)"
                      >
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
                      <el-tag 
                        v-else 
                        :type="req.status === 1 ? 'success' : 'info'" 
                        size="small"
                        round
                      >
                        {{ req.status === 1 ? '已接受' : '已拒绝' }}
                      </el-tag>
                    </div>
                  </el-card>
                </div>
              </div>
            </el-tab-pane>
            
            <el-tab-pane label="发出的请求" name="sent">
              <div v-loading="loading" class="request-list-wrapper">
                <el-empty v-if="!loading && sentRequests.length === 0" description="暂无发出请求" />
                <div v-else class="request-list">
                  <el-card 
                    v-for="req in sentRequests" 
                    :key="req.id" 
                    shadow="hover"
                    class="request-item"
                  >
                    <div class="request-content">
                      <div class="request-info">
                        <div class="request-name">用户ID: {{ req.toUserId }}</div>
                        <div class="request-time">{{ formatTime(req.createTime) }}</div>
                      </div>
                      <el-tag 
                        :type="getStatusType(req.status)" 
                        size="small"
                        round
                      >
                        {{ getStatusText(req.status) }}
                      </el-tag>
                    </div>
                  </el-card>
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
            <el-avatar 
              :size="36" 
              class="clickable-avatar"
              @click.stop="goToUserProfile(user.id)"
            >{{ user.username.charAt(0) }}</el-avatar>
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
import { useRouter } from 'vue-router'
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

const router = useRouter()
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

// 跳转到用户主页
function goToUserProfile(userId) {
  console.log('goToUserProfile called with userId:', userId)
  if (userId) {
    router.push(`/user/${userId}`)
  }
}
</script>

<style scoped lang="scss">
.friends-container {
  max-width: 1400px;
  margin: 0 auto;
}

.friends-card {
  margin-bottom: 24px;
  
  :deep(.el-card__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 20px 24px;
    border-bottom: none;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
      
      .page-title {
        margin: 0;
        font-size: 20px;
        font-weight: 600;
        color: #fff;
      }
    }
  }
  
  .search-bar {
    padding: 16px 24px;
    border-bottom: 1px solid #e5e6eb;
    background: #fafafa;
  }
  
  .friend-list-wrapper {
    padding: 24px;
  }
  
  .friend-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    
    .friend-item {
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      }
      
      .friend-content {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        
        .friend-left {
          display: flex;
          align-items: center;
          gap: 16px;
          flex: 1;
          min-width: 0;
          
          .friend-avatar {
            cursor: pointer;
            border: 2px solid #fff;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
            transition: transform 0.3s;
            
            &:hover {
              transform: scale(1.1);
            }
          }
          
          .friend-info {
            flex: 1;
            min-width: 0;
            
            .friend-name {
              font-size: 16px;
              font-weight: 600;
              color: #1f2329;
              margin-bottom: 4px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
            
            .friend-username {
              font-size: 13px;
              color: #86909c;
              margin-bottom: 4px;
            }
            
            .friend-bio {
              font-size: 13px;
              color: #4e5969;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }
          }
        }
        
        .friend-actions {
          display: flex;
          align-items: center;
          gap: 8px;
          flex-shrink: 0;
        }
      }
    }
  }
}

.requests-card {
  :deep(.el-card__header) {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: #fff;
    padding: 20px 24px;
    border-bottom: none;
  }
  
  .request-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .request-title {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
      color: #fff;
    }
  }
  
  .request-list-wrapper {
    padding: 16px;
  }
  
  .request-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
    
    .request-item {
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      }
      
      .request-content {
        display: flex;
        align-items: center;
        gap: 12px;
        
        .request-info {
          flex: 1;
          min-width: 0;
          
          .request-name {
            font-size: 15px;
            font-weight: 600;
            color: #1f2329;
            margin-bottom: 4px;
          }
          
          .request-message {
            font-size: 13px;
            color: #4e5969;
            margin-bottom: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
          
          .request-time {
            font-size: 12px;
            color: #86909c;
          }
        }
        
        .request-actions {
          display: flex;
          gap: 8px;
          flex-shrink: 0;
        }
      }
    }
  }
}
</style>
