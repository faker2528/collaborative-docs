<template>
  <div class="share-join-container" v-if="!showDocumentPreview">
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
  
  <!-- 文档预览模式 -->
  <div v-else class="document-preview-container">
    <div class="preview-header">
      <h3>{{ linkInfo.documentTitle }}</h3>
      <div class="preview-actions">
        <el-button type="primary" @click="showLoginDialog = true" v-if="!isLoggedIn">
          <el-icon><Edit /></el-icon>
          登录编辑
        </el-button>
        <el-button v-else type="primary" @click="goToDocument">
          <el-icon><Edit /></el-icon>
          进入编辑模式
        </el-button>
      </div>
    </div>
    
    <div class="preview-content" @click="handlePreviewClick">
      <div class="watermark" v-if="!isLoggedIn">受邀预览 - 请登录后进行协作操作</div>
      <div class="document-content" v-html="documentContent"></div>
    </div>
    
    <el-dialog v-model="showLoginDialog" title="登录以编辑文档" width="400px" center>
      <div class="login-prompt">
        <el-icon :size="48" color="#409eff"><UserFilled /></el-icon>
        <p>登录后即可编辑此文档</p>
        <p class="sub-text">登录后自动获得邀请权限</p>
      </div>
      <template #footer>
        <el-button @click="showLoginDialog = false">取消</el-button>
        <el-button type="primary" @click="goToLogin">登录</el-button>
        <el-button type="success" @click="goToRegister">注册</el-button>
      </template>
    </el-dialog>
    
    <!-- 密码验证对话框 -->
    <el-dialog v-model="showPasswordDialog" title="需要密码验证" width="400px" center>
      <div class="password-verification">
        <p>此文档需要密码验证，请输入访问密码：</p>
        <el-input 
          v-model="passwordInput" 
          type="password"
          placeholder="请输入访问密码"
          show-password
          @keyup.enter="verifyPassword"
        />
        <div class="password-error" v-if="passwordError">{{ passwordError }}</div>
      </div>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="verifyPassword" :loading="verifyingPassword">验证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, computed, watch} from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShareLinkInfo, joinByShareLink, getDocumentPreview, verifyShareLinkPassword } from '@/api/document'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const joining = ref(false)
const linkInfo = ref(null)
const error = ref('')
const showLoginDialog = ref(false)
const showDocumentPreview = ref(false)
const documentContent = ref('')

const showPasswordDialog = ref(false)
const passwordInput = ref('')
const passwordError = ref('')
const verifyingPassword = ref(false)
const passwordVerified = ref(false)  // 新增：标记密码是否已验证

const isLoggedIn = computed(() => userStore.isLoggedIn)

// 监听用户登录状态变化，登录后自动加入文档
watch(
  () => userStore.isLoggedIn,
  async (newVal, oldVal) => {
    if (newVal && !oldVal && showDocumentPreview.value) { // 用户从未登录变为已登录，且在预览界面
      console.log('检测到用户登录，自动加入文档')
      // 延迟一下，等待用户信息加载完成
      setTimeout(async () => {
        try {
          await handleJoin()
        } catch (e) {
          console.error('自动加入失败:', e)
        }
      }, 500)
    }
  }
)

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
      } else {
        // 只有当用户已登录且权限为可编辑时，才自动尝试加入
        // 否则显示预览界面让用户自己选择
        if (userStore.isLoggedIn && linkInfo.value.permissionType === 2) {
          // 已登录用户且有编辑权限，询问是否直接加入
          showDocumentPreview.value = true
          await loadDocumentPreview()
        } else {
          // 未登录用户或只读权限，显示预览
          showDocumentPreview.value = true
          await loadDocumentPreview()
        }
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

// 加载文档预览内容
async function loadDocumentPreview() {
  if (linkInfo.value) {
    try {
      const res = await getDocumentPreview(route.params.token) // 使用新的预览接口
      if (res.code === 200 && res.data) {
        // 检查是否需要密码验证（且尚未验证）
        if (res.data.requiresPassword && !passwordVerified.value) {
          showPasswordDialog.value = true
          return
        }
        
        // 直接显示后端返回的 HTML 内容
        let content = res.data.content
        if (!content || content.trim() === '') {
          content = '<p>文档为空</p>'
        }
        
        documentContent.value = content
        console.log('文档预览内容加载成功')
      } else {
        ElMessage.warning(res.msg || '文档内容为空')
        documentContent.value = '<p>文档内容为空</p>'
      }
    } catch (e) {
      console.error('加载文档预览失败:', e)
      ElMessage.error('文档加载失败')
      documentContent.value = '<p>文档加载失败</p>'
    }
  }
}

// 处理预览区域点击（拦截编辑操作）
function handlePreviewClick(event) {
  // 如果用户未登录，拦截编辑相关操作
  if (!userStore.isLoggedIn) {
    // 检查是否点击了编辑相关的元素
    const target = event.target
    if (target.tagName === 'BUTTON' || target.closest('.ql-editor') || target.closest('.editable')) {
      showLoginDialog.value = true
      return
    }
  }
}

// 验证密码
async function verifyPassword() {
  if (!passwordInput.value.trim()) {
    passwordError.value = '请输入密码'
    return
  }
  
  verifyingPassword.value = true
  passwordError.value = ''
  
  try {
    const res = await verifyShareLinkPassword(route.params.token, passwordInput.value)
    if (res.code === 200 && res.data) {
      // 密码验证成功，关闭对话框并标记为已验证
      showPasswordDialog.value = false
      passwordInput.value = ''
      passwordVerified.value = true  // 标记密码已验证
      ElMessage.success('密码验证成功')
      // 重新加载文档内容（此时不会再次弹出密码框）
      await loadDocumentPreview()
    } else {
      passwordError.value = res.msg || '密码错误'
    }
  } catch (e) {
    passwordError.value = '验证失败，请重试'
    console.error('密码验证失败:', e)
  } finally {
    verifyingPassword.value = false
  }
}

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
      // 确保 linkInfo 已加载
      if (linkInfo.value && linkInfo.value.documentId) {
        // 跳转到文档编辑页面
        router.push(`/document/${linkInfo.value.documentId}`)
      } else {
        // 如果 linkInfo 未加载，重新获取
        const infoRes = await getShareLinkInfo(route.params.token)
        if (infoRes.code === 200 && infoRes.data) {
          router.push(`/document/${infoRes.data.documentId}`)
        }
      }
    } else {
      ElMessage.error(res.msg || '加入失败，请重试')
    }
  } catch (e) {
    console.error('加入文档失败:', e)
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

function goToDocument() {
  // 如果是已登录用户，先尝试加入文档再跳转
  if (userStore.isLoggedIn) {
    handleJoin()
  } else {
    // 未登录用户点击时，弹出登录提示
    showLoginDialog.value = true
  }
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

.document-preview-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  
  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    
    h3 {
      margin: 0;
      color: #303133;
      font-size: 18px;
    }
    
    .preview-actions {
      display: flex;
      gap: 8px;
    }
  }
  
  .preview-content {
    flex: 1;
    position: relative;
    padding: 20px;
    overflow-y: auto;
    
    .watermark {
      position: fixed;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%) rotate(-20deg);
      font-size: 24px;
      font-weight: bold;
      color: rgba(0, 0, 0, 0.05);
      pointer-events: none;
      z-index: 1;
      user-select: none;
      white-space: nowrap;
    }
    
    .document-content {
      max-width: 1200px;
      margin: 0 auto;
      background: #fff;
      padding: 40px;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      min-height: calc(100vh - 120px);
      
      :deep(*) {
        user-select: text;
      }
      
      // Quill 编辑器样式
      :deep(.ql-container) {
        font-size: 16px;
        line-height: 1.6;
      }
      
      :deep(.ql-editor) {
        padding: 0;
      }
      
      :deep(p) {
        margin: 8px 0;
      }
      
      :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
        margin: 16px 0 8px;
        font-weight: bold;
      }
      
      :deep(ul), :deep(ol) {
        margin: 8px 0;
        padding-left: 20px;
      }
      
      :deep(blockquote) {
        border-left: 3px solid #ddd;
        padding-left: 16px;
        margin: 8px 0;
        color: #666;
      }
      
      :deep(code) {
        background-color: #f5f5f5;
        padding: 2px 6px;
        border-radius: 3px;
        font-family: monospace;
      }
      
      :deep(pre) {
        background-color: #f5f5f5;
        padding: 12px;
        border-radius: 4px;
        overflow-x: auto;
      }
      
      :deep(img) {
        max-width: 100%;
        height: auto;
      }
    }
  }
}

.password-verification {
  text-align: center;
  
  p {
    margin-bottom: 16px;
    color: #606266;
    font-size: 14px;
  }
  
  .el-input {
    margin: 16px 0;
  }
  
  .password-error {
    color: #f56c6c;
    font-size: 12px;
    margin-top: 8px;
  }
}
</style>
