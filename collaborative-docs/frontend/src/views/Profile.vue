<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人资料</span>
          <el-button v-if="!isEditing" type="primary" size="small" @click="startEdit">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
        </div>
      </template>
      
      <div class="profile-content">
        <div class="avatar-section">
          <el-avatar :size="100" :src="avatarPreview || userStore.userInfo?.avatar">
            {{ userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
          </el-avatar>
          <el-upload
            v-if="isEditing"
            class="avatar-upload"
            :show-file-list="false"
            :http-request="handleAvatarUpload"
            :before-upload="beforeAvatarUpload"
            accept="image/*"
          >
            <el-button size="small" :loading="avatarUploading">更换头像</el-button>
          </el-upload>
        </div>
        
        <el-form 
          ref="formRef" 
          :model="profileForm" 
          :rules="rules"
          label-width="80px"
          class="profile-form"
        >
          <el-form-item label="用户名">
            <span class="info-text">{{ userStore.userInfo?.username }}</span>
            <el-tag size="small" type="info">不可修改</el-tag>
          </el-form-item>
          
          <el-form-item label="昵称" prop="nickname">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.nickname" 
              placeholder="请输入昵称"
            />
            <span v-else class="info-text">{{ userStore.userInfo?.nickname || '-' }}</span>
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.email" 
              placeholder="请输入邮箱"
            />
            <span v-else class="info-text">{{ userStore.userInfo?.email || '-' }}</span>
          </el-form-item>
          
          <el-form-item label="简介" prop="bio">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.bio" 
              type="textarea"
              :rows="3"
              placeholder="介绍一下自己"
            />
            <span v-else class="info-text">{{ userStore.userInfo?.bio || '-' }}</span>
          </el-form-item>
          
          <el-form-item label="注册时间">
            <span class="info-text">{{ formatTime(userStore.userInfo?.createTime) }}</span>
          </el-form-item>
          
          <el-form-item v-if="isEditing">
            <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card class="stats-card">
      <template #header>
        <span>统计信息</span>
      </template>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-value">{{ documentCount }}</div>
          <div class="stat-label">我的文档</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ friendCount }}</div>
          <div class="stat-label">好友数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ sharedCount }}</div>
          <div class="stat-label">共享文档</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useDocumentStore } from '@/stores/document'
import { updateProfile } from '@/api/user'
import { getFriendList } from '@/api/friend'
import request from '@/api/request'

const userStore = useUserStore()
const documentStore = useDocumentStore()

const isEditing = ref(false)
const saving = ref(false)
const avatarUploading = ref(false)
const avatarPreview = ref('')
const formRef = ref()

const profileForm = reactive({
  nickname: '',
  email: '',
  bio: '',
  avatar: ''
})

const rules = {
  nickname: [
    { max: 50, message: '昵称不能超过50个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  bio: [
    { max: 200, message: '简介不能超过200个字符', trigger: 'blur' }
  ]
}

const documentCount = ref(0)
const friendCount = ref(0)
const sharedCount = ref(0)

onMounted(async () => {
  await documentStore.fetchDocuments()
  const userId = userStore.userInfo?.id || userStore.userInfo?.userId
  documentCount.value = documentStore.documents.filter(d => 
    String(d.creatorId) === String(userId)
  ).length
  sharedCount.value = documentStore.documents.filter(d => 
    String(d.creatorId) !== String(userId)
  ).length
  
  try {
    const res = await getFriendList()
    if (res.code === 200) {
      friendCount.value = res.data?.length || 0
    }
  } catch (e) {
    console.error('获取好友列表失败')
  }
})

function startEdit() {
  profileForm.nickname = userStore.userInfo?.nickname || ''
  profileForm.email = userStore.userInfo?.email || ''
  profileForm.bio = userStore.userInfo?.bio || ''
  profileForm.avatar = userStore.userInfo?.avatar || ''
  avatarPreview.value = ''
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
  avatarPreview.value = ''
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

async function handleAvatarUpload({ file }) {
  avatarUploading.value = true
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('businessType', 'avatar')
    
    const res = await request.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (res.code === 200) {
      // 获取文件预览URL（使用preview而不是download，以便在img标签中显示）
      const fileUrl = `/api/file/preview/${res.data.id}`
      profileForm.avatar = fileUrl
      avatarPreview.value = fileUrl
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    console.error('上传头像失败:', e)
    ElMessage.error('上传失败，请重试')
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  saving.value = true
  try {
    const res = await updateProfile(profileForm)
    if (res.code === 200) {
      // 更新本地存储
      userStore.userInfo = {
        ...userStore.userInfo,
        nickname: res.data.nickname,
        email: res.data.email,
        bio: res.data.bio,
        avatar: res.data.avatar
      }
      localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
      
      ElMessage.success('资料更新成功')
      isEditing.value = false
      avatarPreview.value = ''
    }
  } catch (e) {
    ElMessage.error('更新失败，请重试')
  } finally {
    saving.value = false
  }
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN')
}
</script>

<style scoped lang="scss">
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .profile-content {
    display: flex;
    gap: 40px;
    
    .avatar-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }
    
    .profile-form {
      flex: 1;
      
      .info-text {
        color: #606266;
        margin-right: 8px;
      }
    }
  }
}

.stats-card {
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    
    .stat-item {
      text-align: center;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 8px;
      
      .stat-value {
        font-size: 32px;
        font-weight: bold;
        color: #409eff;
      }
      
      .stat-label {
        margin-top: 8px;
        color: #909399;
        font-size: 14px;
      }
    }
  }
}
</style>
