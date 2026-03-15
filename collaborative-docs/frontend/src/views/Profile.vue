<template>
  <div class="profile-container">
    <!-- 个人资料卡片 -->
    <el-card shadow="hover" class="profile-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h2 class="page-title">个人资料</h2>
          </div>
          <el-button 
            v-if="!isEditing" 
            type="primary" 
            @click="startEdit"
          >
            <el-icon><Edit /></el-icon>
            编辑资料
          </el-button>
        </div>
      </template>
      
      <div class="profile-content">
        <!-- 头像区域 -->
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <el-avatar 
              :size="120" 
              :src="avatarPreview || userStore.userInfo?.avatar"
              class="profile-avatar"
            >
              {{ userStore.userInfo?.nickname?.charAt(0) || userStore.userInfo?.username?.charAt(0) }}
            </el-avatar>
            <div class="avatar-overlay" v-if="isEditing">
              <el-upload
                :show-file-list="false"
                :http-request="handleAvatarUpload"
                :before-upload="beforeAvatarUpload"
                accept="image/*"
              >
                <el-button 
                  circle 
                  size="large"
                  :loading="avatarUploading"
                  class="upload-btn"
                >
                  <el-icon><Camera /></el-icon>
                </el-button>
              </el-upload>
            </div>
          </div>
          <div class="user-name-section" v-if="!isEditing">
            <h3 class="display-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h3>
            <p class="username">@{{ userStore.userInfo?.username }}</p>
          </div>
        </div>
        
        <!-- 表单区域 -->
        <el-form 
          ref="formRef" 
          :model="profileForm" 
          :rules="rules"
          label-width="100px"
          class="profile-form"
        >
          <el-form-item label="用户名">
            <div class="info-display">
              <span class="info-text">{{ userStore.userInfo?.username }}</span>
              <el-tag size="small" type="info" round>不可修改</el-tag>
            </div>
          </el-form-item>
          
          <el-form-item label="昵称" prop="nickname">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.nickname" 
              placeholder="请输入昵称"
              size="large"
            />
            <div v-else class="info-display">
              <span class="info-text">{{ userStore.userInfo?.nickname || '-' }}</span>
            </div>
          </el-form-item>
          
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.email" 
              placeholder="example@email.com"
              size="large"
            />
            <div v-else class="info-display">
              <span class="info-text">{{ userStore.userInfo?.email || '-' }}</span>
            </div>
          </el-form-item>
          
          <el-form-item label="个人简介" prop="bio">
            <el-input 
              v-if="isEditing"
              v-model="profileForm.bio" 
              type="textarea"
              :rows="4"
              placeholder="介绍一下自己，让更多人了解你"
              maxlength="200"
              show-word-limit
            />
            <div v-else class="info-display bio-text">
              {{ userStore.userInfo?.bio || '这个人很懒，什么都没写~' }}
            </div>
          </el-form-item>
          
          <el-form-item label="注册时间">
            <div class="info-display">
              <el-icon><Calendar /></el-icon>
              <span class="info-text">{{ formatTime(userStore.userInfo?.createTime) }}</span>
            </div>
          </el-form-item>
          
          <el-form-item v-if="isEditing" class="form-actions">
            <el-space>
              <el-button type="primary" :loading="saving" @click="saveProfile">
                <el-icon><Check /></el-icon>
                保存更改
              </el-button>
              <el-button @click="cancelEdit">
                <el-icon><Close /></el-icon>
                取消
              </el-button>
            </el-space>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <!-- 统计信息卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon document">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ documentCount }}</div>
              <div class="stat-label">创建的文档</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon friend">
              <el-icon :size="32"><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ friendCount }}</div>
              <div class="stat-label">好友数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-icon share">
              <el-icon :size="32"><Share /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ sharedCount }}</div>
              <div class="stat-label">共享的文档</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
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
  max-width: 1000px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card {
  :deep(.el-card__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    padding: 20px 24px;
    border-bottom: none;
  }
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      .page-title {
        margin: 0;
        font-size: 24px;
        font-weight: 600;
        color: #fff;
      }
    }
  }
  
  .profile-content {
    padding: 32px 24px;
    display: flex;
    gap: 48px;
    
    @media (max-width: 768px) {
      flex-direction: column;
      gap: 32px;
    }
    
    .avatar-section {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
      
      .avatar-wrapper {
        position: relative;
        
        .profile-avatar {
          border: 4px solid #fff;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
          transition: all 0.3s;
        }
        
        .avatar-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          border-radius: 50%;
          background: rgba(0, 0, 0, 0.5);
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: opacity 0.3s;
          
          &:hover {
            opacity: 1;
          }
          
          .upload-btn {
            color: #fff;
            background: rgba(255, 255, 255, 0.2);
            backdrop-filter: blur(10px);
            
            &:hover {
              background: rgba(255, 255, 255, 0.3);
            }
          }
        }
      }
      
      .user-name-section {
        text-align: center;
        
        .display-name {
          margin: 0 0 4px;
          font-size: 20px;
          font-weight: 600;
          color: #1f2329;
        }
        
        .username {
          margin: 0;
          font-size: 14px;
          color: #86909c;
        }
      }
    }
    
    .profile-form {
      flex: 1;
      
      .info-display {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        
        .info-text {
          color: #4e5969;
        }
        
        .el-icon {
          color: #86909c;
          margin-right: 4px;
        }
      }
      
      .bio-text {
        line-height: 1.8;
        white-space: pre-wrap;
      }
      
      .form-actions {
        margin-top: 24px;
        padding-top: 24px;
        border-top: 1px solid #e5e6eb;
      }
    }
  }
}

.stats-row {
  .stat-card {
    margin-bottom: 20px;
    
    :deep(.el-card__body) {
      padding: 24px;
    }
    
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
      
      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        &.document {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: #fff;
        }
        
        &.friend {
          background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
          color: #fff;
        }
        
        &.share {
          background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
          color: #fff;
        }
      }
      
      .stat-info {
        flex: 1;
        
        .stat-value {
          font-size: 28px;
          font-weight: 700;
          color: #1f2329;
          margin-bottom: 4px;
        }
        
        .stat-label {
          font-size: 14px;
          color: #86909c;
        }
      }
    }
  }
}
</style>
