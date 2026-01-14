import request from './request'

// 上传文件
export function uploadFile(file, businessType = '', businessId = '') {
  const formData = new FormData()
  formData.append('file', file)
  if (businessType) formData.append('businessType', businessType)
  if (businessId) formData.append('businessId', businessId)
  
  return request.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (progressEvent) => {
      const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total)
      console.log('上传进度:', percentCompleted + '%')
    }
  })
}

// 获取文件信息
export function getFileInfo(fileId) {
  return request.get(`/file/${fileId}`)
}

// 删除文件
export function deleteFile(fileId) {
  return request.delete(`/file/${fileId}`)
}

// 获取用户上传的文件列表
export function getUserFiles(businessType = '', page = 1, size = 20) {
  return request.get('/file/list', {
    params: { businessType, page, size }
  })
}

// 获取文件下载链接
export function getDownloadUrl(fileId) {
  return `/api/file/download/${fileId}`
}

// 获取文件预览链接（用于图片、PDF等直接在浏览器中显示）
export function getPreviewUrl(fileId) {
  return `/api/file/preview/${fileId}`
}
