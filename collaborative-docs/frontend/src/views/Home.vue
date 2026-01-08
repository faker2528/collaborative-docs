<template>
  <div class="home-container">
    <div class="home-header">
      <h2>我的文档</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        新建文档
      </el-button>
    </div>
    
    <el-card v-loading="documentStore.loading">
      <el-empty v-if="documentStore.documents.length === 0" description="暂无文档">
        <el-button type="primary" @click="showCreateDialog">创建第一个文档</el-button>
      </el-empty>
      
      <div v-else class="document-grid">
        <div 
          v-for="doc in documentStore.documents" 
          :key="doc.id" 
          class="document-card"
          @click="openDocument(doc.id)"
        >
          <div class="document-icon">
            <el-icon :size="48"><Document /></el-icon>
          </div>
          <div class="document-info">
            <h4 class="document-title">{{ doc.title }}</h4>
            <p class="document-time">{{ formatTime(doc.updateTime) }}</p>
          </div>
          <div class="document-actions" @click.stop>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, doc)">
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
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 创建文档对话框 -->
    <el-dialog v-model="createDialogVisible" title="新建文档" width="400px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules">
        <el-form-item prop="title" label="文档标题">
          <el-input v-model="createForm.title" placeholder="请输入文档标题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDocumentStore } from '@/stores/document'

const router = useRouter()
const documentStore = useDocumentStore()

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
  max-width: 1200px;
  margin: 0 auto;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  
  h2 {
    margin: 0;
    color: #303133;
  }
}

.document-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.document-card {
  position: relative;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  
  &:hover {
    background: #f0f0f0;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  .document-icon {
    text-align: center;
    color: #409eff;
    margin-bottom: 12px;
  }
  
  .document-info {
    text-align: center;
    
    .document-title {
      margin: 0 0 8px;
      font-size: 14px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .document-time {
      margin: 0;
      font-size: 12px;
      color: #909399;
    }
  }
  
  .document-actions {
    position: absolute;
    top: 8px;
    right: 8px;
    opacity: 0;
    transition: opacity 0.3s;
  }
  
  &:hover .document-actions {
    opacity: 1;
  }
}
</style>
