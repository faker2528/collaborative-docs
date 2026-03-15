<template>
  <div class="home-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">你好，{{ userStore.userProfile?.nickname || userStore.userProfile?.username }}</h1>
        <p class="welcome-subtitle">开始你的高效协作之旅</p>
      </div>
      <el-button type="primary" size="large" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建文档
      </el-button>
    </div>
    
    <!-- 快捷操作区 -->
    <div class="quick-actions">
      <el-card shadow="hover" class="action-card" @click="showCreateDialog">
        <div class="action-icon primary">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="action-info">
          <h3>新建文档</h3>
          <p>创建协作文档</p>
        </div>
      </el-card>
      
      <el-card shadow="hover" class="action-card" @click="$router.push('/files')">
        <div class="action-icon success">
          <el-icon :size="24"><Folder /></el-icon>
        </div>
        <div class="action-info">
          <h3>文件管理</h3>
          <p>管理所有文件</p>
        </div>
      </el-card>
      
      <el-card shadow="hover" class="action-card" @click="$router.push('/friends')">
        <div class="action-icon warning">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="action-info">
          <h3>好友协作</h3>
          <p>邀请好友协作</p>
        </div>
      </el-card>
      
      <el-card shadow="hover" class="action-card" @click="$router.push('/messages')">
        <div class="action-icon info">
          <el-icon :size="24"><ChatDotRound /></el-icon>
        </div>
        <div class="action-info">
          <h3>消息中心</h3>
          <p>查看消息通知</p>
        </div>
      </el-card>
    </div>
    
    <!-- 最近文档 -->
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">最近文档</h2>
        <el-button text @click="$router.push('/documents')">
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      
      <el-card v-loading="documentStore.loading" shadow="never">
        <el-empty v-if="documentStore.documents.length === 0" description="暂无文档">
          <el-button type="primary" @click="showCreateDialog">创建第一个文档</el-button>
        </el-empty>
        
        <el-table v-else :data="documentStore.documents" style="width: 100%" :header-cell-style="{ background: '#fafafa', color: '#606266' }">
          <el-table-column prop="title" label="文档名称" min-width="300">
            <template #default="{ row }">
              <div class="doc-name-cell" @click="openDocument(row.id)">
                <el-icon :size="20" color="#409eff"><Document /></el-icon>
                <span class="doc-title">{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" label="最后修改时间" width="180" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click="openDocument(row.id)">编辑</el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
                <el-button type="text" size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="delete">
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    
    <!-- 创建文档对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建文档" width="500px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item prop="title" label="文档标题">
          <el-input 
            v-model="createForm.title" 
            placeholder="请输入文档标题，如：产品需求文档"
            size="large"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="handleCreate">立即创建</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDocumentStore } from '@/stores/document'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const documentStore = useDocumentStore()
const userStore = useUserStore()

const createDialogVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()

const createForm = reactive({
  title: ''
})

const createRules = {
  title: [
    { required: true, message: '请输入文档标题', trigger: 'blur' }
  ]
}

onMounted(() => {
  documentStore.fetchDocuments()
})

function showCreateDialog() {
  createForm.title = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    const res = await documentStore.addDocument(createForm.title)
    if (res.code === 200) {
      ElMessage.success('文档创建成功')
      createDialogVisible.value = false
      
      // 添加短暂延迟以确保文档被完全创建
      await new Promise(resolve => setTimeout(resolve, 300))
      
      router.push(`/document/${res.data.id}`)
      console.log('跳转到文档页面，ID:', res.data.id, '类型:', typeof res.data.id)
    }
  } finally {
    creating.value = false
  }
}

function openDocument(id) {
  router.push(`/document/${id}`)
}

async function handleCommand(command, doc) {
  if (command === 'delete') {
    try {
      await ElMessageBox.confirm(`确定要删除文档"${doc.title}"吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      const res = await documentStore.removeDocument(doc.id)
      if (res.code === 200) {
        ElMessage.success('文档删除成功')
      }
    } catch {
      // 取消操作
    }
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.home-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

// 欢迎区域
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  
  .welcome-content {
    .welcome-title {
      margin: 0 0 8px;
      font-size: 32px;
      font-weight: 600;
      letter-spacing: -0.5px;
    }
    
    .welcome-subtitle {
      margin: 0;
      font-size: 16px;
      opacity: 0.9;
    }
  }
}

// 快捷操作区
.quick-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
  
  .action-card {
    cursor: pointer;
    transition: all 0.3s;
    border-radius: 12px;
    border: none;
    overflow: hidden;
    
    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
    }
    
    :deep(.el-card__body) {
      padding: 24px;
      display: flex;
      align-items: center;
      gap: 16px;
    }
    
    .action-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      
      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
      }
      
      &.success {
        background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
        color: #fff;
      }
      
      &.warning {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: #fff;
      }
      
      &.info {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
        color: #fff;
      }
    }
    
    .action-info {
      flex: 1;
      min-width: 0;
      
      h3 {
        margin: 0 0 4px;
        font-size: 16px;
        font-weight: 600;
        color: #1f2329;
      }
      
      p {
        margin: 0;
        font-size: 13px;
        color: #86909c;
      }
    }
  }
}

// 内容区块
.section {
  margin-bottom: 32px;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .section-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #1f2329;
    }
    
    :deep(.el-button) {
      font-size: 14px;
      color: #409eff;
      
      &:hover {
        color: #66b1ff;
      }
    }
  }
}

// 表格样式
.doc-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  
  .doc-title {
    font-weight: 500;
    color: #1f2329;
    
    &:hover {
      color: #409eff;
    }
  }
}

.time-text {
  color: #86909c;
  font-size: 13px;
}

// 对话框
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
