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
        
        <el-sub-menu index="documents">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>文档管理</span>
          </template>
          <el-menu-item index="/documents">
            <el-icon><Document /></el-icon>
            <template #title>我的文档</template>
          </el-menu-item>
          <el-menu-item index="/documents/shared">
            <el-icon><Share /></el-icon>
            <template #title>共享给我</template>
          </el-menu-item>
        </el-sub-menu>
        
        <el-sub-menu index="profile">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/profile">
            <el-icon><UserFilled /></el-icon>
            <template #title>个人资料</template>
          </el-menu-item>
          <el-menu-item index="/friends">
            <el-icon><Avatar /></el-icon>
            <template #title>
              <span>我的好友</span>
              <el-badge v-if="pendingCount > 0" :value="pendingCount" class="badge" />
            </template>
          </el-menu-item>
        </el-sub-menu>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getPendingRequestCount } from '@/api/friend'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const pendingCount = ref(0)

const activeMenu = computed(() => {
  return route.path
})

const currentRoute = computed(() => {
  const routeMap = {
    '/': '',
    '/documents': '我的文档',
    '/documents/shared': '共享给我',
    '/profile': '个人资料',
    '/friends': '我的好友'
  }
  
  if (route.path.startsWith('/document/')) {
    return '文档编辑'
  }
  
  return routeMap[route.path] || ''
})

onMounted(async () => {
  await fetchPendingCount()
})

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
  
  .logo-container {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    
    .logo-icon {
      font-size: 28px;
      color: #409eff;
    }
    
    .logo-text {
      margin-left: 10px;
      font-size: 18px;
      font-weight: bold;
      color: #fff;
      white-space: nowrap;
    }
  }
  
  .side-menu {
    flex: 1;
    border-right: none;
    background: transparent;
    
    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: #bfcbd9;
      
      &:hover {
        background-color: rgba(64, 158, 255, 0.1);
      }
      
      .el-icon {
        color: #bfcbd9;
      }
    }
    
    :deep(.el-menu-item.is-active) {
      color: #409eff;
      background-color: rgba(64, 158, 255, 0.2);
      
      .el-icon {
        color: #409eff;
      }
    }
    
    :deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
      color: #fff;
    }
    
    .badge {
      margin-left: 8px;
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
    
    &:hover {
      color: #409eff;
      background-color: rgba(64, 158, 255, 0.1);
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
  padding: 0 20px;
  height: 60px;
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      
      .username {
        margin: 0 8px;
        color: #606266;
      }
    }
  }
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
