<template>
  <el-container class="layout-container">
    <!-- 左侧菜单 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo-container">
        <el-icon class="logo-icon"><Document /></el-icon>
        <span v-show="!isCollapse" class="logo-text">协作文档</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        class="side-menu"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        
        <el-menu-item index="/file-explorer">
          <el-icon><Folder /></el-icon>
          <template #title>文件管理</template>
        </el-menu-item>
        
        <el-menu-item index="/friends">
          <el-icon><User /></el-icon>
          <template #title>
            <span>我的好友</span>
            <el-badge v-if="pendingCount > 0" :value="pendingCount" class="badge" />
          </template>
        </el-menu-item>
        
        <el-menu-item index="/messages">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>
            <span>消息中心</span>
            <el-badge v-if="unreadMessageCount > 0" :value="unreadMessageCount" class="badge" />
          </template>
        </el-menu-item>
      </el-menu>
      
      <div class="collapse-btn" @click="isCollapse = !isCollapse">
        <el-icon><component :is="isCollapse ? 'Expand' : 'Fold'" /></el-icon>
      </div>
    </el-aside>
    
    <!-- 右侧内容区 -->
    <el-container class="main-container">
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute">
              {{ currentRoute }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 消息图标 -->
          <el-badge :value="unreadMessageCount" :hidden="unreadMessageCount === 0" class="message-badge">
            <el-tooltip content="消息中心" placement="bottom">
              <el-button circle @click="goToMessages">
                <el-icon :size="20"><ChatDotRound /></el-icon>
              </el-button>
            </el-tooltip>
          </el-badge>
          
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPendingRequestCount } from '@/api/friend'
import { getUnreadCount } from '@/api/message'
import { messageWs } from '@/utils/messageWebSocket'
import { eventBus, Events } from '@/utils/eventBus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const pendingCount = ref(0)
const unreadMessageCount = ref(0)

const activeMenu = computed(() => {
  return route.path
})

const currentRoute = computed(() => {
  const routeMap = {
    '/': '',
    '/documents': '我的文档',
    '/documents/shared': '共享给我',
    '/profile': '个人资料',
    '/friends': '我的好友',
    '/messages': '消息中心'
  }
  
  if (route.path.startsWith('/document/')) {
    return '文档编辑'
  }
  
  return routeMap[route.path] || ''
})

onMounted(async () => {
  await fetchPendingCount()
  await fetchUnreadMessageCount()
  
  // 连接消息WebSocket
  const userId = userStore.userInfo?.userId || userStore.userInfo?.id
  if (userId) {
    messageWs.connect(userId)
  }
  
  // 监听未读数变化事件
  eventBus.on(Events.UNREAD_COUNT_CHANGED, handleUnreadCountChanged)
  eventBus.on(Events.FRIEND_REQUEST_CHANGED, handleFriendRequestChanged)
})

onUnmounted(() => {
  messageWs.disconnect()
  eventBus.off(Events.UNREAD_COUNT_CHANGED, handleUnreadCountChanged)
  eventBus.off(Events.FRIEND_REQUEST_CHANGED, handleFriendRequestChanged)
})

function handleUnreadCountChanged() {
  // 重新获取未读数
  fetchUnreadMessageCount()
}

function handleFriendRequestChanged() {
  // 重新获取好友请求数
  fetchPendingCount()
}

async function fetchUnreadMessageCount() {
  try {
    const res = await getUnreadCount()
    if (res.code === 200) {
      unreadMessageCount.value = res.data.count || 0
    }
  } catch (e) {
    console.error('获取未读消息数失败')
  }
}

async function fetchPendingCount() {
  try {
    const res = await getPendingRequestCount()
    if (res.code === 200) {
      pendingCount.value = res.data.count || 0
    }
  } catch (e) {
    console.error('获取待处理请求数失败')
  }
}

async function handleCommand(command) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logoutAction()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消操作
    }
  } else if (command === 'profile') {
    router.push('/profile')
  }
}

function goToMessages() {
  router.push('/messages')
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100%;
}

.layout-aside {
  background: linear-gradient(180deg, #1d1e1f 0%, #2d3a4b 100%);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  
  .logo-container {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 20px;
    background: rgba(0, 0, 0, 0.2);
    
    .logo-icon {
      font-size: 32px;
      color: #409eff;
    }
    
    .logo-text {
      margin-left: 12px;
      font-size: 20px;
      font-weight: 600;
      color: #fff;
      white-space: nowrap;
      letter-spacing: 1px;
    }
  }
  
  .side-menu {
    flex: 1;
    border-right: none;
    background: transparent;
    overflow-y: auto;
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: #bfcbd9;
      height: 56px;
      line-height: 56px;
      margin: 4px 8px;
      border-radius: 8px;
      
      &:hover {
        background-color: rgba(64, 158, 255, 0.1);
      }
      
      .el-icon {
        color: #bfcbd9;
        font-size: 18px;
      }
    }
    
    :deep(.el-menu-item.is-active) {
      color: #409eff;
      background: linear-gradient(90deg, rgba(64, 158, 255, 0.2) 0%, transparent 100%);
      
      .el-icon {
        color: #409eff;
      }
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        background: #409eff;
      }
    }
    
    .badge {
      margin-left: 8px;
      :deep(.el-badge__content) {
        transform: translateY(-20%) translateX(50%);
      }
    }
  }
  
  .collapse-btn {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #bfcbd9;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.3s;
    
    &:hover {
      color: #409eff;
      background-color: rgba(64, 158, 255, 0.1);
    }
    
    .el-icon {
      font-size: 18px;
    }
  }
}

.main-container {
  flex-direction: column;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  height: 64px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  
  .header-left {
    :deep(.el-breadcrumb) {
      font-size: 14px;
      
      .el-breadcrumb__inner {
        color: #606266;
        
        &:hover {
          color: #409eff;
        }
      }
      
      .el-breadcrumb__separator {
        color: #C0C4CC;
      }
    }
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .message-badge {
      :deep(.el-badge__content) {
        transform: translateY(-50%) translateX(50%);
      }
    }
    
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 8px;
      transition: all 0.3s;
      
      &:hover {
        background: #f5f7fa;
      }
      
      .username {
        margin: 0 8px;
        color: #606266;
        font-weight: 500;
      }
    }
  }
}

.layout-main {
  background: #f5f7fa;
  padding: 24px;
  overflow-y: auto;
}
</style>
