<template>
  <div class="file-manager-container">
    <!-- 左侧文件夹树 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <el-icon><Folder /></el-icon>
        <span>文件目录</span>
      </div>
      
      <!-- 搜索框 -->
      <div class="search-box">
        <el-input 
          v-model="searchKeyword" 
          placeholder="搜索文件或文件夹" 
          size="small"
          clearable
          prefix-icon="Search"
        />
      </div>
      
      <!-- 树形文件夹 -->
      <div class="folder-tree">
        <el-tree
          ref="treeRef"
          :data="folderTree"
          :props="treeProps"
          default-expand-all
          highlight-current
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <el-icon class="folder-icon">
                <component :is="data.type === 'folder' ? 'FolderOpened' : 'Document'" />
              </el-icon>
              <span class="tree-node-label">{{ node.label }}</span>
            </div>
          </template>
        </el-tree>
      </div>
      
      <!-- 底部操作按钮 -->
      <div class="sidebar-footer">
        <el-button type="primary" style="width: 100%;" @click="showCreateFolderDialog">
          <el-icon><Plus /></el-icon>
          新建文件夹
        </el-button>
        <el-button style="width: 100%; margin-top: 8px;" @click="showUploadDialog">
          <el-icon><Upload /></el-icon>
          上传文件
        </el-button>
      </div>
    </aside>
    
    <!-- 右侧文件列表 -->
    <main class="main-content">
      <!-- 面包屑导航 -->
      <div class="breadcrumb-bar">
        <div class="breadcrumb-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item @click="goToRoot" style="cursor: pointer;">
              <el-icon><HomeFilled /></el-icon>
              全部文件
            </el-breadcrumb-item>
            <el-breadcrumb-item 
              v-for="(folder, index) in currentPath" 
              :key="folder.id"
              @click="handleBreadcrumbClick(folder, index)"
              style="cursor: pointer;"
            >
              {{ folder.name }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="view-mode">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="list">
              <el-icon><List /></el-icon>
            </el-radio-button>
            <el-radio-button value="grid">
              <el-icon><Grid /></el-icon>
            </el-radio-button>
          </el-radio-group>
        </div>
      </div>
      
      <!-- 文件列表 -->
      <div v-loading="loading" class="file-list-wrapper">
        <el-empty 
          v-if="!loading && filteredFileList.length === 0" 
          description="暂无文件"
        >
          <el-button type="primary" @click="showUploadDialog">上传文件</el-button>
          <el-button type="success" @click="showCreateFolderDialog">新建文件夹</el-button>
        </el-empty>
        
        <!-- 列表视图 -->
        <el-table 
          v-show="viewMode === 'list'" 
          :data="filteredFileList" 
          style="width: 100%"
          :header-cell-style="{ background: '#fafafa', color: '#606266' }"
          @row-dblclick="handleRowDblClick"
        >
          <el-table-column prop="originalName" label="文件名" min-width="300">
            <template #default="{ row }">
              <div class="file-name-cell">
                <el-icon :size="24" :color="getFileIconColor(row)">
                  <component :is="getFileIcon(row)" />
                </el-icon>
                <span class="file-title">{{ row.originalName || row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="fileSize" label="大小" width="120" align="center">
            <template #default="{ row }">
              <span class="file-size">{{ formatFileSize(row.fileSize || row.size) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="上传时间" width="180" align="center">
            <template #default="{ row }">
              <span class="time-text">{{ formatTime(row.createTime || row.updateTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" text size="small" @click.stop="handlePreview(row)">预览</el-button>
              <el-button type="success" text size="small" @click.stop="handleDownload(row)">下载</el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleFileCommand(cmd, row)">
                <el-button type="text" size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      <el-icon><Edit /></el-icon>
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item command="move">
                      <el-icon><FolderAdd /></el-icon>
                      移动到
                    </el-dropdown-item>
                    <el-dropdown-item command="share">
                      <el-icon><Share /></el-icon>
                      分享
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 网格视图 -->
        <div v-show="viewMode === 'grid'" class="file-grid">
          <div 
            v-for="file in filteredFileList" 
            :key="file.id" 
            class="file-card"
            @dblclick="handleFileOpen(file)"
          >
            <div class="file-card-header">
              <el-icon :size="48" :color="getFileIconColor(file)">
                <component :is="getFileIcon(file)" />
              </el-icon>
              <el-dropdown trigger="click" @command="(cmd) => handleFileCommand(cmd, file)">
                <el-button text circle size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
              </el-dropdown>
            </div>
            <div class="file-card-body">
              <div class="file-card-name" :title="file.originalName || file.name">{{ file.originalName || file.name }}</div>
              <div class="file-card-meta">
                <span>{{ formatFileSize(file.fileSize || file.size) }}</span>
                <span>·</span>
                <span>{{ formatTime(file.createTime || file.updateTime) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    
    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="createFolderDialogVisible" title="新建文件夹" width="400px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules">
        <el-form-item prop="name" label="文件夹名称">
          <el-input v-model="createForm.name" placeholder="请输入文件夹名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateFolder">创建</el-button>
      </template>
    </el-dialog>
    
    <!-- 文件预览对话框 -->
    <el-dialog 
      v-model="previewDialogVisible" 
      :title="previewFileName"
      width="80%"
      top="5vh"
      destroy-on-close
    >
      <div class="preview-container">
        <img v-if="previewType === 'image'" :src="previewUrl" class="preview-image" alt="预览图片" />
        <iframe v-else-if="previewType === 'pdf'" :src="previewUrl" class="preview-pdf" />
        <video v-else-if="previewType === 'video'" :src="previewUrl" controls class="preview-video" />
        <audio v-else-if="previewType === 'audio'" :src="previewUrl" controls class="preview-audio" />
        <div v-else class="preview-unsupported">
          <el-icon :size="64"><Document /></el-icon>
          <p>该文件类型不支持在线预览</p>
          <el-button type="primary" @click="handleDownload(previewFile)">下载文件</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserFiles, deleteFile, getPreviewUrl, uploadFile } from '@/api/file'
import request from '@/api/request'

// 数据
const searchKeyword = ref('')
const viewMode = ref('list') // 'list' or 'grid'
const loading = ref(false)
const folderTree = ref([])
const fileList = ref([])
const currentPath = ref([])
const currentFolderId = ref(null) // null 表示根目录

// 树配置
const treeProps = {
  children: 'children',
  label: 'name',
  isLeaf: (data) => data.type === 'file'
}

// 对话框相关
const createFolderDialogVisible = ref(false)
const createFormRef = ref()
const createForm = reactive({
  name: ''
})
const createRules = {
  name: [
    { required: true, message: '请输入文件夹名称', trigger: 'blur' },
    { min: 1, max: 50, message: '文件夹名称长度在 1 到 50 个字符之间', trigger: 'blur' }
  ]
}

// 上传相关
const uploadDialogVisible = ref(false)
const uploadProgress = ref(0)
const uploadingFile = ref(null)

// 预览相关
const previewDialogVisible = ref(false)
const previewFileName = ref('')
const previewUrl = ref('')
const previewType = ref('')
const previewFile = ref(null)

// 计算属性：过滤后的文件列表
const filteredFileList = computed(() => {
  if (!searchKeyword.value) return fileList.value
  const keyword = searchKeyword.value.toLowerCase()
  return fileList.value.filter(file => 
    file.name.toLowerCase().includes(keyword)
  )
})

onMounted(() => {
  loadFolderTree()
  loadFiles()
})

// 加载文件夹树
async function loadFolderTree() {
  try {
    // TODO: 调用后端接口加载文件夹树
    // const res = await request.get('/file/folders')
    // folderTree.value = res.data || []
    
    // 暂时使用空数组，等待后端接口
    folderTree.value = []
  } catch (e) {
    console.error('加载文件夹树失败:', e)
  }
}

// 加载文件列表
async function loadFiles() {
  loading.value = true
  try {
    const res = await getUserFiles('', 1, 100)
    if (res.code === 200) {
      // 根据是否有 folderId 参数决定加载根目录还是子目录
      if (currentFolderId.value) {
        // 加载指定文件夹的内容
        // TODO: 调用后端接口加载子目录文件
        // const res = await request.get(`/file/folder/${currentFolderId.value}`)
        fileList.value = res.data?.list || []
      } else {
        // 加载根目录文件
        fileList.value = res.data?.list || []
      }
    } else {
      ElMessage.error(res.msg || '加载文件列表失败')
    }
  } catch (e) {
    console.error('加载文件列表失败:', e)
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

// 点击树节点
function handleNodeClick(data, node) {
  if (data.type === 'folder') {
    currentFolderId.value = data.id
    // 更新面包屑路径
    updateCurrentPath(node)
    loadFiles()
  }
}

// 更新当前路径（面包屑）
function updateCurrentPath(node) {
  const path = []
  let currentNode = node
  
  while (currentNode) {
    path.unshift({
      id: currentNode.data.id,
      name: currentNode.data.name
    })
    currentNode = currentNode.parent
  }
  
  currentPath.value = path
}

// 点击面包屑导航
function handleBreadcrumbClick(folder, index) {
  if (index === currentPath.value.length - 1) {
    return // 点击当前目录，不处理
  }
  
  // 截取到点击的文件夹
  currentPath.value = currentPath.value.slice(0, index + 1)
  currentFolderId.value = folder.id
  loadFiles()
}

// 回到根目录
function goToRoot() {
  currentFolderId.value = null
  currentPath.value = []
  loadFiles()
}

// 显示创建文件夹对话框
function showCreateFolderDialog() {
  createForm.name = ''
  createFolderDialogVisible.value = true
}

// 创建文件夹
async function handleCreateFolder() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  
  try {
    // TODO: 调用后端接口创建文件夹
    // const res = await request.post('/file/folder', {
    //   name: createForm.name,
    //   parentFolderId: currentFolderId.value
    // })
    
    ElMessage.success('文件夹创建成功')
    createFolderDialogVisible.value = false
    loadFolderTree()
    if (currentFolderId.value) {
      loadFiles()
    }
  } catch (e) {
    console.error('创建文件夹失败:', e)
    ElMessage.error('创建文件夹失败')
  }
}

// 显示上传对话框
function showUploadDialog() {
  // 触发文件选择
  const input = document.createElement('input')
  input.type = 'file'
  input.onchange = (e) => {
    const file = e.target.files[0]
    if (file) {
      handleFileUpload(file)
    }
  }
  input.click()
}

// 处理文件上传
async function handleFileUpload(file) {
  uploadDialogVisible.value = true
  uploadingFile.value = file
  uploadProgress.value = 0
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    if (currentFolderId.value) {
      formData.append('parentFolderId', currentFolderId.value)
    }
    
    const res = await uploadFile(file, '', '', (progress) => {
      uploadProgress.value = progress
    })
    
    if (res.code === 200) {
      ElMessage.success('文件上传成功')
      loadFiles()
      loadFolderTree()
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    console.error('上传文件失败:', e)
    ElMessage.error('上传失败，请重试')
  } finally {
    uploadDialogVisible.value = false
    uploadingFile.value = null
    uploadProgress.value = 0
  }
}

// 获取文件图标
function getFileIcon(file) {
  const ext = getExtension(file.name).toLowerCase()
  const iconMap = {
    'doc': 'Document',
    'docx': 'Document',
    'pdf': 'Document',
    'xls': 'Office',
    'xlsx': 'Office',
    'ppt': 'Office',
    'pptx': 'Office',
    'png': 'Picture',
    'jpg': 'Picture',
    'jpeg': 'Picture',
    'gif': 'Picture',
    'bmp': 'Picture',
    'mp4': 'VideoCamera',
    'avi': 'VideoCamera',
    'mov': 'VideoCamera',
    'mp3': 'Headset',
    'wav': 'Headset',
    'zip': 'Files',
    'rar': 'Files',
    '7z': 'Files',
    'txt': 'Document',
    'md': 'Document'
  }
  return iconMap[ext] || 'Document'
}

// 获取文件扩展名
function getExtension(filename) {
  const parts = filename.split('.')
  return parts.length > 1 ? parts[parts.length - 1] : ''
}

// 获取文件图标颜色
function getFileIconColor(file) {
  const ext = getExtension(file.name).toLowerCase()
  const colorMap = {
    'doc': '#409EFF',
    'docx': '#409EFF',
    'pdf': '#F56C6C',
    'xls': '#67C23A',
    'xlsx': '#67C23A',
    'ppt': '#E6A23C',
    'pptx': '#E6A23C',
    'png': '#909399',
    'jpg': '#909399',
    'jpeg': '#909399',
    'gif': '#909399',
    'bmp': '#909399',
    'mp4': '#E6A23C',
    'avi': '#E6A23C',
    'mov': '#E6A23C',
    'mp3': '#409EFF',
    'wav': '#409EFF',
    'zip': '#909399',
    'rar': '#909399',
    '7z': '#909399',
    'txt': '#409EFF',
    'md': '#409EFF'
  }
  return colorMap[ext] || '#909399'
}

// 格式化文件大小
function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

// 格式化时间
function formatTime(time) {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 双击行
function handleRowDblClick(row) {
  handleFileOpen(row)
}

// 打开文件
function handleFileOpen(file) {
  handlePreview(file)
}

// 预览文件
function handlePreview(file) {
  previewFileName.value = file.originalName || file.name
  previewFile.value = file
  
  const ext = getExtension(file.originalName || file.name).toLowerCase()
  
  // 判断文件类型
  if (['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(ext)) {
    previewType.value = 'image'
    previewUrl.value = getPreviewUrl(file.id)
  } else if (ext === 'pdf') {
    previewType.value = 'pdf'
    previewUrl.value = getPreviewUrl(file.id)
  } else if (['mp4', 'avi', 'mov'].includes(ext)) {
    previewType.value = 'video'
    previewUrl.value = getPreviewUrl(file.id)
  } else if (['mp3', 'wav'].includes(ext)) {
    previewType.value = 'audio'
    previewUrl.value = getPreviewUrl(file.id)
  } else {
    previewType.value = 'other'
  }
  
  previewDialogVisible.value = true
}

// 下载文件
async function handleDownload(file) {
  try {
    // 创建临时链接触发下载
    const link = document.createElement('a')
    link.href = `/api/file/download/${file.id}`
    link.download = file.originalName || file.name
    link.target = '_blank'
    link.click()
    
    ElMessage.success('开始下载')
  } catch (e) {
    console.error('下载失败:', e)
    ElMessage.error('下载失败')
  }
}

// 文件操作菜单
function handleFileCommand(command, file) {
  switch (command) {
    case 'rename':
      handleRename(file)
      break
    case 'move':
      handleMove(file)
      break
    case 'share':
      handleShare(file)
      break
    case 'delete':
      handleDelete(file)
      break
  }
}

// 重命名文件
async function handleRename(file) {
  // TODO: 实现重命名功能
  ElMessage.info('重命名功能开发中')
}

// 移动文件
async function handleMove(file) {
  // TODO: 实现移动功能
  ElMessage.info('移动功能开发中')
}

// 分享文件
async function handleShare(file) {
  // TODO: 实现分享功能
  ElMessage.info('分享功能开发中')
}

// 删除文件
async function handleDelete(file) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文件"${file.originalName || file.name}"吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await deleteFile(file.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadFiles()
      loadFolderTree()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败:', e)
    }
  }
}
</script>

<style scoped lang="scss">
.file-manager-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

// 左侧边栏
.sidebar {
  width: 280px;
  background: #fafafa;
  border-right: 1px solid #e5e6eb;
  display: flex;
  flex-direction: column;
  
  .sidebar-header {
    padding: 20px;
    font-size: 16px;
    font-weight: 600;
    color: #1f2329;
    display: flex;
    align-items: center;
    gap: 8px;
    border-bottom: 1px solid #e5e6eb;
    
    .el-icon {
      color: #409eff;
    }
  }
  
  .search-box {
    padding: 16px;
    border-bottom: 1px solid #e5e6eb;
  }
  
  .folder-tree {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    
    .tree-node {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .folder-icon {
        color: #409eff;
      }
      
      .tree-node-label {
        font-size: 14px;
      }
    }
  }
  
  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid #e5e6eb;
  }
}

// 主内容区
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  
  .breadcrumb-bar {
    padding: 16px 24px;
    border-bottom: 1px solid #e5e6eb;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .view-mode {
      :deep(.el-radio-button) {
        margin-left: 8px;
      }
    }
  }
  
  .file-list-wrapper {
    flex: 1;
    overflow-y: auto;
    padding: 24px;
  }
}

// 文件网格
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  
  .file-card {
    padding: 20px;
    background: #fafafa;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.3s;
    
    &:hover {
      background: #f0f0f0;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }
    
    .file-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    
    .file-card-body {
      .file-card-name {
        font-size: 14px;
        font-weight: 500;
        color: #1f2329;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
      
      .file-card-meta {
        font-size: 12px;
        color: #86909c;
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }
  }
}

// 表格样式
.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .file-title {
    font-weight: 500;
    color: #1f2329;
  }
}

.file-size, .time-text {
  color: #86909c;
  font-size: 13px;
}

// 预览对话框
.preview-container {
  text-align: center;
  
  .preview-image {
    max-width: 100%;
    max-height: 70vh;
    object-fit: contain;
  }
  
  .preview-pdf {
    width: 100%;
    height: 70vh;
    border: none;
  }
  
  .preview-video, .preview-audio {
    max-width: 100%;
  }
  
  .preview-unsupported {
    padding: 40px;
    color: #86909c;
    
    .el-icon {
      margin-bottom: 16px;
    }
  }
}
</style>
