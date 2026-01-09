<template>
  <div class="share-join-container">
    <el-card v-loading="loading" class="share-card">
      <template #header>
        <div class="card-header">
          <el-icon :size="24"><Link /></el-icon>
          <span>分享链接</span>
        </div>
      </template>
      
      <div v-if="linkInfo" class="link-info">
        <div class="document-preview">
          <el-icon :size="64"><Document /></el-icon>
          <h2>{{ linkInfo.documentTitle }}</h2>
          <el-tag :type="linkInfo.permissionType === 2 ? 'success' : 'info'">
            {{ linkInfo.permissionType === 2 ? '可编辑' : '只读' }}
          </el-tag>
        </div>
        
        <div class="link-meta">
          <p v-if="linkInfo.expireTime">
            <el-icon><Clock /></el-icon>
            过期时间: {{ formatTime(linkInfo.expireTime) }}
          </p>
          <p v-if="linkInfo.maxUses > 0">
            <el-icon><User /></el-icon>
            使用次数: {{ linkInfo.usedCount }} / {{ linkInfo.maxUses }}
          </p>
        </div>
        
        <el-button type="primary" size="large" @click="handleJoin" :loading="joining">
          加入文档协作
        </el-button>
      </div>
      
      <div v-else-if="error" class="error-info">
        <el-icon :size="64" class="error-icon"><CircleCloseFilled /></el-icon>
        <h3>{{ error }}</h3>
        <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
      </div>
    </el-card>
    
    <!-- 登录提示弹窗 -->
    <el-dialog v-model="showLoginDialog" title="需要登录" width="400px" center>
      <div class="login-prompt">
        <el-icon :size="48" color="#409eff"><UserFilled /></el-icon>
        <p>加入文档协作需要登录账号</p>
        <p class="sub-text">登录后将自动加入此文档</p>
      </div>
      <template #footer>
        <el-button @click="showLoginDialog = false">取消</el-button>
        <el-button type="primary" @click="goToLogin">去登录</el-button>
        <el-button type="success" @click="goToRegister">注册账号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShareLinkInfo, joinByShareLink } from '@/api/document'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const joining = ref(false)
const linkInfo = ref(null)
const error = ref('')
const showLoginDialog = ref(false)

onMounted(async () => {
  const token = route.params.token
  if (!token) {
    error.value = '无效的分享链接'
    loading.value = false
    return
  }
  
  try {
    const res = await getShareLinkInfo(token)
    if (res.code === 200 && res.data) {
      linkInfo.value = res.data
      
      // 检查链接状态
      if (linkInfo.value.status !== 1) {
        error.value = '该分享链接已失效'
        linkInfo.value = null
      }
    } else {
      error.value = res.msg || '分享链接不存在'
    }
  } catch (e) {
    error.value = '获取链接信息失败'
  } finally {
    loading.value = false
  }
})

async function handleJoin() {
  // 检查是否已登录
  if (!userStore.isLoggedIn) {
    showLoginDialog.value = true
    return
  }
  
  joining.value = true
  try {
    const res = await joinByShareLink(route.params.token)
    if (res.code === 200) {
      ElMessage.success('已成功加入文档协作')
      router.push(`/document/${linkInfo.value.documentId}`)
    }
  } catch (e) {
    ElMessage.error('加入失败，请重试')
  } finally {
    joining.value = false
  }
}

function goToLogin() {
  // 保存当前分享链接，登录后跳回
  router.push({ name: 'Login', query: { redirect: route.fullPath } })
}

function goToRegister() {
  router.push({ name: 'Register', query: { redirect: route.fullPath } })
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.share-join-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.share-card {
  width: 500px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 18px;
    font-weight: bold;
  }
  
  .link-info {
    text-align: center;
    
    .document-preview {
      padding: 30px 0;
      
      .el-icon {
        color: #409eff;
        margin-bottom: 16px;
      }
      
      h2 {
        margin: 0 0 12px;
        color: #303133;
      }
    }
    
    .link-meta {
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
      margin-bottom: 24px;
      
      p {
        margin: 8px 0;
        color: #606266;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
      }
    }
  }
  
  .error-info {
    text-align: center;
    padding: 40px 0;
    
    .error-icon {
      color: #f56c6c;
      margin-bottom: 16px;
    }
    
    h3 {
      color: #909399;
      margin-bottom: 24px;
    }
  }
}

.login-prompt {
  text-align: center;
  padding: 20px 0;
  
  p {
    margin: 16px 0 8px;
    font-size: 16px;
    color: #303133;
  }
  
  .sub-text {
    color: #909399;
    font-size: 14px;
  }
}
</style>
