<template>
  <div class="user-profile-container" v-loading="loading">
    <el-card v-if="userInfo" class="profile-card">
      <div class="profile-header">
        <el-avatar :size="100">{{ userInfo.username?.charAt(0) }}</el-avatar>
        <div class="user-basic-info">
          <h2>{{ userInfo.nickname || userInfo.username }}</h2>
          <p class="username">@{{ userInfo.username }}</p>
          <p class="email" v-if="userInfo.email">{{ userInfo.email }}</p>
        </div>
      </div>
      
      <div class="profile-content">
        <div v-if="userInfo.bio" class="bio-section">
          <h4>个人简介</h4>
          <p>{{ userInfo.bio }}</p>
        </div>
        
        <div class="info-section">
          <p v-if="userInfo.createTime">
            <el-icon><Calendar /></el-icon>
            注册时间: {{ formatTime(userInfo.createTime) }}
          </p>
        </div>
      </div>
      
      <!-- 操作按钮 -->
      <div class="profile-actions" v-if="!isCurrentUser">
        <el-button 
          v-if="friendStatus === 'none'"
          type="primary" 
          @click="handleAddFriend"
          :loading="actionLoading"
        >
          <el-icon><Plus /></el-icon>
          添加好友
        </el-button>
        <el-button v-else-if="friendStatus === 'pending'" type="info" disabled>
          <el-icon><Clock /></el-icon>
          请求已发送
        </el-button>
        <el-button v-else-if="friendStatus === 'friend'" type="success" disabled>
          <el-icon><Check /></el-icon>
          已是好友
        </el-button>
      </div>
      
      <div class="profile-actions" v-if="isCurrentUser">
        <el-button type="primary" @click="$router.push('/profile')">
          <el-icon><Edit /></el-icon>
          编辑资料
        </el-button>
      </div>
    </el-card>
    
    <el-empty v-else-if="!loading" description="用户不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserById } from '@/api/user'
import { sendFriendRequest, getFriendList, getPendingRequests } from '@/api/friend'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const actionLoading = ref(false)
const userInfo = ref(null)
const friendStatus = ref('none') // none, pending, friend

const isCurrentUser = computed(() => {
  const currentUserId = userStore.userInfo?.userId || userStore.userInfo?.id
  const profileUserId = userInfo.value?.id || userInfo.value?.userId
  return currentUserId && profileUserId && String(currentUserId) === String(profileUserId)
})

// 监听路由参数变化，重新加载用户信息
watch(() => route.params.id, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    await loadUserInfo()
    if (!isCurrentUser.value && userInfo.value) {
      await checkFriendStatus()
    }
  }
}, { immediate: false })

onMounted(async () => {
  await loadUserInfo()
  if (!isCurrentUser.value && userInfo.value) {
    await checkFriendStatus()
  }
})

async function loadUserInfo() {
  loading.value = true
  try {
    const userId = route.params.id
    const res = await getUserById(userId)
    if (res.code === 200) {
      userInfo.value = res.data
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  } finally {
    loading.value = false
  }
}

async function checkFriendStatus() {
  try {
    // 检查是否已是好友
    const friendRes = await getFriendList()
    if (friendRes.code === 200) {
      const isFriend = friendRes.data?.some(f => f.id === userInfo.value.id)
      if (isFriend) {
        friendStatus.value = 'friend'
        return
      }
    }
    
    // 检查是否有待处理的请求
    const pendingRes = await getPendingRequests()
    if (pendingRes.code === 200) {
      const hasPending = pendingRes.data?.some(r => 
        r.receiverId === userInfo.value.id || r.senderId === userInfo.value.id
      )
      if (hasPending) {
        friendStatus.value = 'pending'
        return
      }
    }
    
    friendStatus.value = 'none'
  } catch (e) {
    console.error('检查好友状态失败:', e)
  }
}

async function handleAddFriend() {
  actionLoading.value = true
  try {
    const res = await sendFriendRequest({ receiverId: userInfo.value.id })
    if (res.code === 200) {
      ElMessage.success('好友请求已发送')
      friendStatus.value = 'pending'
    }
  } catch (e) {
    ElMessage.error('发送请求失败')
  } finally {
    actionLoading.value = false
  }
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.user-profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.profile-card {
  .profile-header {
    display: flex;
    align-items: center;
    gap: 24px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
    
    .user-basic-info {
      h2 {
        margin: 0 0 8px;
        font-size: 24px;
        color: #303133;
      }
      
      .username {
        color: #909399;
        margin: 0 0 4px;
      }
      
      .email {
        color: #606266;
        font-size: 14px;
        margin: 0;
      }
    }
  }
  
  .profile-content {
    padding: 20px 0;
    
    .bio-section {
      margin-bottom: 20px;
      
      h4 {
        margin: 0 0 8px;
        color: #606266;
      }
      
      p {
        color: #303133;
        line-height: 1.6;
      }
    }
    
    .info-section {
      p {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #909399;
        font-size: 14px;
      }
    }
  }
  
  .profile-actions {
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    text-align: center;
  }
}
</style>
