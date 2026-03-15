# 在线协作文档系统 - API 接口文档

## 基础信息

- **Base URL**: `/api`
- **认证方式**: JWT Token（通过请求头 `X-User-Id` 传递用户 ID）
- **数据格式**: JSON

---

## 用户模块 (User)

### 1. 用户登录
**POST** `/user/login`

**请求参数**:
```json
{
  "username": "string",
  "password": "string"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "string",
    "userId": "string",
    "username": "string"
  }
}
```

---

### 2. 用户注册
**POST** `/user/register`

**请求参数**:
```json
{
  "username": "string",
  "password": "string",
  "nickname": "string" // 可选
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": "string",
    "username": "string"
  }
}
```

---

### 3. 用户登出
**POST** `/user/logout`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 4. 获取当前用户信息
**GET** `/user/info`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "username": "string",
    "nickname": "string",
    "bio": "string",
    "avatarUrl": "string",
    "email": "string"
  }
}
```

---

### 5. 根据 ID 获取用户信息
**GET** `/user/{userId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "username": "string",
    "nickname": "string",
    "bio": "string",
    "avatarUrl": "string"
  }
}
```

---

### 6. 搜索用户
**GET** `/user/search`

**请求参数**:
- `keyword`: string - 搜索关键词

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "username": "string",
      "nickname": "string",
      "avatarUrl": "string"
    }
  ]
}
```

---

### 7. 更新个人资料
**PUT** `/user/profile`

**请求参数**:
```json
{
  "nickname": "string",
  "bio": "string",
  "email": "string"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

## 文档模块 (Document)

### 1. 获取文档列表
**GET** `/document/list`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "title": "string",
      "creatorId": "string",
      "content": "string",
      "createTime": "datetime",
      "updateTime": "datetime"
    }
  ]
}
```

---

### 2. 获取文档详情
**GET** `/document/{id}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "title": "string",
    "creatorId": "string",
    "content": "string",
    "createTime": "datetime",
    "updateTime": "datetime"
  }
}
```

---

### 3. 创建文档
**POST** `/document`

**请求参数**:
```json
{
  "title": "string",
  "content": "string" // Quill Delta JSON 字符串
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "title": "string",
    "creatorId": "string"
  }
}
```

---

### 4. 更新文档
**PUT** `/document/{id}`

**请求体**: text/plain - Quill Delta JSON 字符串

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 5. 删除文档
**DELETE** `/document/{id}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 6. 分享文档给指定用户
**POST** `/document/{id}/share`

**请求参数**:
- `targetUserId`: string - 目标用户 ID
- `permissionType`: number - 权限类型（1:只读，2:可编辑）

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 7. 获取文档成员列表
**GET** `/document/{id}/members`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "userId": "string",
      "username": "string",
      "nickname": "string",
      "avatarUrl": "string",
      "permissionType": "number"
    }
  ]
}
```

---

### 8. 更新成员权限
**PUT** `/document/{id}/members/{targetUserId}`

**请求参数**:
- `permissionType`: number - 权限类型（1:只读，2:可编辑）

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 9. 移除成员
**DELETE** `/document/{id}/members/{targetUserId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

## 分享链接模块 (Share Link)

### 1. 创建分享链接
**POST** `/share/link`

**请求参数**:
```json
{
  "documentId": "string",
  "permissionType": "number", // 1:只读，2:可编辑
  "expireDays": "number", // 过期天数，0 为永久
  "maxUses": "number", // 最大使用次数，0 为不限制
  "verificationType": "number", // 验证类型（0:免验证，1:密码，2:邮箱）
  "password": "string", // 密码（当 verificationType=1 时）
  "email": "string" // 邮箱（当 verificationType=2 时）
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "documentId": "string",
    "documentTitle": "string",
    "token": "string",
    "shareUrl": "string",
    "permissionType": "number",
    "expireTime": "datetime",
    "maxUses": "number",
    "usedCount": "number",
    "status": "number",
    "verificationType": "number"
  }
}
```

---

### 2. 通过分享链接加入文档
**POST** `/share/join/{token}`

**路径参数**:
- `token`: string - 分享链接 token

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 3. 获取文档的分享链接列表
**GET** `/share/links/{documentId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "documentId": "string",
      "documentTitle": "string",
      "token": "string",
      "shareUrl": "string",
      "permissionType": "number",
      "expireTime": "datetime",
      "maxUses": "number",
      "usedCount": "number",
      "status": "number",
      "verificationType": "number"
    }
  ]
}
```

---

### 4. 禁用分享链接
**DELETE** `/share/link/{linkId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 5. 获取分享链接信息
**GET** `/share/info/{token}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "documentId": "string",
    "title": "string",
    "permissionType": "number",
    "creatorId": "string",
    "verificationType": "number"
  }
}
```

---

### 6. 获取文档预览信息
**GET** `/share/preview/{token}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "documentId": "string",
    "title": "string",
    "permissionType": "number",
    "creatorId": "string",
    "verificationType": "number",
    "requiresPassword": "boolean",
    "content": "string", // HTML 格式
    "createTime": "datetime"
  }
}
```

---

### 7. 验证分享链接密码
**POST** `/share/verify-password/{token}`

**请求参数**:
```json
{
  "password": "string"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true // boolean
}
```

---

## 文件模块 (File)

### 1. 上传文件
**POST** `/file/upload`

**请求参数** (FormData):
- `file`: File - 文件对象
- `businessType`: string - 业务类型（可选）
- `businessId`: string - 业务 ID（可选）

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "fileName": "string",
    "fileUrl": "string",
    "fileSize": "number",
    "fileType": "string"
  }
}
```

---

### 2. 获取文件信息
**GET** `/file/{fileId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "fileName": "string",
    "fileUrl": "string",
    "fileSize": "number",
    "fileType": "string",
    "uploadTime": "datetime"
  }
}
```

---

### 3. 删除文件
**DELETE** `/file/{fileId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 4. 获取用户文件列表
**GET** `/file/list`

**请求参数**:
- `businessType`: string - 业务类型（可选）
- `page`: number - 页码（默认 1）
- `size`: number - 每页数量（默认 20）

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": "number",
    "list": [
      {
        "id": "string",
        "fileName": "string",
        "fileUrl": "string",
        "fileSize": "number",
        "fileType": "string"
      }
    ]
  }
}
```

---

### 5. 获取文件下载链接
**GET** `/file/download/{fileId}`

**响应**: 文件下载流

---

### 6. 获取文件预览链接
**GET** `/file/preview/{fileId}`

**响应**: 文件预览流（用于图片、PDF等直接在浏览器中显示）

---

## 好友模块 (Friend)

### 1. 获取好友列表
**GET** `/friend/list`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "friendId": "string",
      "username": "string",
      "nickname": "string",
      "avatarUrl": "string",
      "remark": "string"
    }
  ]
}
```

---

### 2. 发送好友请求
**POST** `/friend/request`

**请求参数**:
```json
{
  "toUserId": "string",
  "message": "string"
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 3. 获取收到的好友请求
**GET** `/friend/requests/received`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "fromUserId": "string",
      "fromUser": {
        "id": "string",
        "username": "string",
        "nickname": "string",
        "avatarUrl": "string"
      },
      "message": "string",
      "createTime": "datetime"
    }
  ]
}
```

---

### 4. 获取发送的好友请求
**GET** `/friend/requests/sent`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "toUserId": "string",
      "toUser": {
        "id": "string",
        "username": "string",
        "nickname": "string",
        "avatarUrl": "string"
      },
      "message": "string",
      "createTime": "datetime",
      "status": "number"
    }
  ]
}
```

---

### 5. 获取待处理的好友请求数量
**GET** `/friend/requests/pending/count`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "count": "number"
  }
}
```

---

### 6. 同意好友请求
**POST** `/friend/request/{requestId}/accept`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 7. 拒绝好友请求
**POST** `/friend/request/{requestId}/reject`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 8. 删除好友
**DELETE** `/friend/{friendId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 9. 检查是否为好友
**GET** `/friend/check/{targetUserId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "isFriend": "boolean"
  }
}
```

---

## 消息模块 (Message)

### 1. 获取会话列表
**GET** `/message/conversations`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": "string",
      "userId": "string",
      "user": {
        "id": "string",
        "username": "string",
        "nickname": "string",
        "avatarUrl": "string"
      },
      "lastMessage": {
        "id": "string",
        "content": "string",
        "sendTime": "datetime"
      },
      "unreadCount": "number"
    }
  ]
}
```

---

### 2. 获取或创建会话
**GET** `/message/conversation/with/{targetUserId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "userId": "string",
    "user": {
      "id": "string",
      "username": "string",
      "nickname": "string",
      "avatarUrl": "string"
    }
  }
}
```

---

### 3. 获取会话消息列表
**GET** `/message/conversation/{conversationId}/messages`

**请求参数**:
- `page`: number - 页码（默认 1）
- `size`: number - 每页数量（默认 20）

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": "number",
    "list": [
      {
        "id": "string",
        "senderId": "string",
        "receiverId": "string",
        "content": "string",
        "messageType": "string",
        "sendTime": "datetime",
        "isRead": "boolean"
      }
    ]
  }
}
```

---

### 4. 发送消息
**POST** `/message/send`

**请求参数**:
```json
{
  "receiverId": "string",
  "content": "string",
  "messageType": "string" // "text", "image", "file" 等
}
```

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": "string",
    "sendTime": "datetime"
  }
}
```

---

### 5. 标记会话已读
**POST** `/message/conversation/{conversationId}/read`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 6. 删除会话
**DELETE** `/message/conversation/{conversationId}`

**响应**:
```json
{
  "code": 200,
  "msg": "success"
}
```

---

### 7. 获取未读消息总数
**GET** `/message/unread/count`

**响应**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "count": "number"
  }
}
```

---

## WebSocket 实时通信

### 连接地址
`ws://localhost:8080/ws/messages`

### 连接参数
- 需要在请求头中携带 `X-User-Id`

### 消息格式
```json
{
  "type": "string", // "new_message", "friend_request", "system" 等
  "data": {}
}
```

---

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权，需要登录 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 数据字典

### PermissionType（权限类型）
| 值 | 说明 |
|----|------|
| 1 | 只读 |
| 2 | 可编辑 |

### VerificationType（验证类型）
| 值 | 说明 |
|----|------|
| 0 | 免验证 |
| 1 | 密码验证 |
| 2 | 邮箱验证 |

### MessageType（消息类型）
| 值 | 说明 |
|----|------|
| text | 文本消息 |
| image | 图片消息 |
| file | 文件消息 |
| system | 系统消息 |

---

*最后更新时间：2026-03-15*
