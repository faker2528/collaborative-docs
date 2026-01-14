<template>
  <div class="file-manager">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件传输管理</span>
          <el-upload
            :show-file-list="false"
            :before-upload="handleBeforeUpload"
            :http-request="handleUpload"
          >
            <el-button type="primary">
              <el-icon><Upload /></el-icon>
              上传文件
            </el-button>
          </el-upload>
        </div>
      </template>
      
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select v-model="filterType" placeholder="文件类型" clearable style="width: 150px;">
          <el-option label="全部" value="" />
          <el-option label="聊天文件" value="chat" />
          <el-option label="文档附件" value="document" />
          <el-option label="头像" value="avatar" />
        </el-select>
      </div>
      
      <!-- 文件列表 -->
      <el-table :data="files" v-loading="loading" style="width: 100%">
        <el-table-column prop="originalName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon :size="20"><Document /></el-icon>
              <span>{{ row.originalName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileType" label="类型" width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ getFileTypeLabel(row.fileType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="用途" width="100">
          <template #default="{ row }">
            {{ getBusinessTypeLabel(row.businessType) }}
          </template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载次数" width="100" />
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="handleDownload(row)">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button type="danger" size="small" text @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchFiles"
        />
      </div>
    </el-card>
    
    <!-- 上传进度对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="400px" :close-on-click-modal="false">
      <div class="upload-progress">
        <p>{{ uploadFileName }}</p>
        <el-progress :percentage="uploadProgress" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserFiles, deleteFile, getDownloadUrl } from '@/api/file'
import request from '@/api/request'

const files = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterType = ref('')

const uploadDialogVisible = ref(false)
const uploadFileName = ref('')
const uploadProgress = ref(0)

onMounted(() => {
  fetchFiles()
})

watch(filterType, () => {
  currentPage.value = 1
  fetchFiles()
})

async function fetchFiles() {
  loading.value = true
  try {
    const res = await getUserFiles(filterType.value, currentPage.value, pageSize.value)
    if (res.code === 200) {
      files.value = res.data || []
      // 假设后端返回总数
      total.value = files.value.length
    }
  } catch (e) {
    console.error('获取文件列表失败:', e)
  } finally {
    loading.value = false
  }
}

function handleBeforeUpload(file) {
  const maxSize = 100 * 1024 * 1024 // 100MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过100MB')
    return false
  }
  return true
}

async function handleUpload({ file }) {
  uploadFileName.value = file.name
  uploadProgress.value = 0
  uploadDialogVisible.value = true
  
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const res = await request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
      }
    })
    
    if (res.code === 200) {
      ElMessage.success('上传成功')
      uploadDialogVisible.value = false
      fetchFiles()
    }
  } catch (e) {
    ElMessage.error('上传失败')
    uploadDialogVisible.value = false
  }
}

function handleDownload(file) {
  const url = getDownloadUrl(file.id)
  const link = document.createElement('a')
  link.href = url
  link.download = file.originalName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

async function handleDelete(file) {
  try {
    await ElMessageBox.confirm(`确定要删除文件"${file.originalName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteFile(file.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchFiles()
    }
  } catch (e) {
    // 取消或失败
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let index = 0
  let size = bytes
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index++
  }
  return size.toFixed(1) + ' ' + units[index]
}

function getFileTypeLabel(mimeType) {
  if (!mimeType) return '未知'
  if (mimeType.startsWith('image/')) return '图片'
  if (mimeType.includes('pdf')) return 'PDF'
  if (mimeType.includes('word') || mimeType.includes('document')) return 'Word'
  if (mimeType.includes('excel') || mimeType.includes('sheet')) return 'Excel'
  if (mimeType.includes('zip') || mimeType.includes('rar')) return '压缩包'
  if (mimeType.startsWith('text/')) return '文本'
  return '文件'
}

function getBusinessTypeLabel(type) {
  const labels = {
    chat: '聊天',
    document: '文档',
    avatar: '头像'
  }
  return labels[type] || '其他'
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.file-manager {
  max-width: 1200px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 16px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.upload-progress {
  text-align: center;
  
  p {
    margin-bottom: 16px;
    color: #606266;
  }
}
</style>
