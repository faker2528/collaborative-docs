# 协作文档系统 - Collaborative Document System

## 项目简介

基于微服务架构的在线协作文档编辑系统，支持多人实时协同编辑、即时通讯、文件传输等功能。系统采用前后端分离设计，后端使用 Spring Cloud 微服务框架，前端采用 Vue 3 + Element Plus 技术栈。

### 核心特性

- 📝 **多人实时协同编辑** - 基于 CRDT 算法实现无冲突编辑
- 💬 **即时通讯系统** - 支持好友聊天、陌生人私信、系统消息
- 📄 **文档权限管理** - 细粒度的文档权限控制（查看/编辑/管理）
- 🔗 **邀请链接分享** - 支持免登录预览、操作触发登录的轻量化协作流程
- 📁 **文件传输管理** - 统一文件上传下载接口，支持本地/MinIO/OSS 存储
- 📜 **版本历史管理** - 完整的文档版本控制和历史回溯
- 👥 **好友关系管理** - 好友申请、添加、删除等社交功能

## 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.0.0-RC1 | 阿里微服务生态 |
| Nacos | 2.2.3 | 服务注册与配置中心 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存中间件 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| JWT | 0.12.3 | 身份认证 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 渐进式 JavaScript 框架 |
| Vite | 5.0.10 | 前端构建工具 |
| Vue Router | 4.2.5 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Element Plus | 2.4.4 | UI 组件库 |
| Axios | 1.6.2 | HTTP 客户端 |
| Quill | 1.3.7 | 富文本编辑器 |

### 微服务模块

```
collaborative-docs/
├── common                  # 公共模块（实体类、DTO、工具类等）
├── gateway-service         # 网关服务（端口：8080）
├── user-service           # 用户服务（端口：8081）
├── document-service       # 文档服务（端口：8082）
├── collaboration-service  # 协作服务（端口：8083）
├── history-service        # 历史服务（端口：8084）
├── message-service        # 消息服务
├── file-service           # 文件服务
└── frontend               # 前端项目（端口：3000）
```

#### 服务职责说明

1. **Gateway Service (网关服务)**
   - 统一入口，负责请求路由、鉴权、限流
   - WebSocket 协议转发
   - JWT 令牌验证

2. **User Service (用户服务)**
   - 用户注册、登录、个人信息管理
   - 好友关系管理（添加、删除、列表）
   - 好友请求处理

3. **Document Service (文档服务)**
   - 文档 CRUD 操作
   - 文档权限管理
   - 分享链接生成与管理

4. **Collaboration Service (协作服务)**
   - 多人实时协同编辑（WebSocket）
   - CRDT 算法实现数据一致性
   - 在线用户状态管理

5. **History Service (历史服务)**
   - 文档版本历史管理
   - 版本对比与回滚
   - 操作日志记录

6. **Message Service (消息服务)**
   - 即时通讯（WebSocket）
   - 消息推送与存储
   - 会话管理

7. **File Service (文件服务)**
   - 文件上传/下载
   - 本地/MinIO存储适配
   - 文件传输记录管理

## 快速开始

### 环境要求

- **JDK**: 17+
- **Maven**: 3.6+
- **Node.js**: 18+
- **Docker & Docker Compose**: 最新稳定版

### 方式一：Docker 一键启动（推荐）

```bash
# 进入项目目录
cd collaborative-docs

# 启动所有服务（包括基础设施和业务服务）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 停止所有服务
docker-compose down
```

### 方式二：本地开发模式

#### 1. 启动基础设施

```bash
# Windows
start-infra.bat

# Linux/Mac
./start-infra.sh

# 或手动执行
docker-compose up -d mysql redis nacos
```

等待约 30 秒让 Nacos 完全启动，访问 http://localhost:8848/nacos 验证（账号：nacos/nacos）

#### 2. 编译后端项目

```bash
# 在项目根目录
mvn clean package -DskipTests
```

#### 3. 安装前端依赖

```bash
cd frontend
npm install
```

#### 4. 启动各微服务（分别在不同终端窗口）

```bash
# 终端 1 - 网关服务
cd gateway-service
mvn spring-boot:run

# 终端 2 - 用户服务
cd user-service
mvn spring-boot:run

# 终端 3 - 文档服务
cd document-service
mvn spring-boot:run

# 终端 4 - 协作服务
cd collaboration-service
mvn spring-boot:run

# 终端 5 - 历史服务
cd history-service
mvn spring-boot:run

# 终端 6 - 消息服务
cd message-service
mvn spring-boot:run

# 终端 7 - 文件服务
cd file-service
mvn spring-boot:run
```

#### 5. 启动前端开发服务器

```bash
cd frontend
npm run dev
```

#### 6. 访问系统

- **前端地址**: http://localhost:3000
- **测试账号**: 
  - admin / 123456
  - test / 123456
- **Nacos控制台**: http://localhost:8848/nacos
- **MySQL**: localhost:3306 (root/root)
- **Redis**: localhost:6379

## 数据库设计

### 核心数据表

| 表名 | 说明 |
|------|------|
| t_user | 用户表 |
| t_document | 文档表 |
| t_document_permission | 文档权限表 |
| t_document_history | 文档版本历史表 |
| t_friendship | 好友关系表 |
| t_friend_request | 好友请求表 |
| t_share_link | 文档分享链接表 |
| t_conversation | 会话表 |
| t_message | 消息表 |
| t_system_notification | 系统通知表 |
| t_file_record | 文件记录表 |

详细表结构请查看 `scripts/init.sql`

## 核心功能说明

### 1. 文档协作编辑

- 基于 **CRDT（Conflict-free Replicated Data Type）** 算法实现无冲突协同编辑
- 实时显示其他用户的编辑内容和光标位置
- 自动保存和版本控制
- 支持文字、图片、表格等富文本格式

### 2. 邀请链接分享

#### 链接结构
```
http://localhost:3000/share?token=xxx&docId=xxx
```

#### 访问流程
- **未登录用户**: 点击链接 → 免登录预览 → 执行操作时弹窗引导登录 → 登录后自动恢复操作
- **已登录用户**: 点击链接 → 直接获得对应权限

#### 配置选项
- 权限类型：仅预览 / 可编辑
- 有效期：永久 / 24 小时 / 7 天 / 自定义
- 访问次数限制：无限制 / 5 次 / 10 次 / 自定义
- 验证方式：免验证 / 密码验证 / 邮箱验证

### 3. 即时通讯

#### 消息类型
- **系统消息**: 平台通知、功能更新、文档操作提醒
- **好友消息**: 与已添加好友的一对一聊天
- **陌生人消息**: 与非好友用户的临时聊天
- **申请消息**: 好友申请、文档权限申请、协作邀请

#### 聊天功能
- 文字消息、表情符号
- 文件传输
- 消息状态（发送中/已送达/已读）
- 历史消息加载

### 4. 文件传输管理

#### 存储方式
- **本地存储**: 小文件、临时文件
- **MinIO 分布式存储**: 大文件、生产环境
- **第三方OSS**: 预留阿里云 OSS、腾讯云 COS 接口

#### 使用场景
- 聊天窗口文件发送
- 文档附件上传
- 头像和个人资料上传

### 5. 权限管理

#### 文档权限级别
- **查看权限**: 仅可查看文档内容
- **编辑权限**: 可编辑文档内容
- **管理权限**: 可管理文档成员和权限设置

#### 权限规则
- 只有文档创建者能赋予其他成员权限
- 其他成员邀请或分享的，默认为只读权限
- 支持按用户粒度设置权限

## 常见问题

### Q1: 在线人数显示不准确

**问题描述**: B 退出文档编辑时，其他成员界面显示的在线人数仍存在 B 用户

**解决方案**: 
1. 检查 WebSocket 连接是否正常关闭
2. 确认后端是否正确广播用户离开事件
3. 前端需监听连接断开事件并主动更新

### Q2: Token 失效后无法自动跳转登录

**解决方案**: 
在网关层统一拦截 401 错误，前端全局捕获后跳转到登录页

### Q3: 分享链接访问时前端显示空白

**原因**: 预览页面未正确解析文档内容

**解决方案**: 
确保后端返回的是渲染后的 HTML 文本，而非原始 JSON 数据

### Q4: 消息推送不及时

**解决方案**: 
1. 检查 WebSocket 连接状态
2. 确认消息服务的消息推送机制
3. 前端需建立心跳检测机制

## 开发与贡献

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Checkstyle 进行代码风格检查
- 提交前请运行单元测试

### Git 分支策略

- `main`: 主分支，生产环境代码
- `dev`: 开发分支
- `feature/*`: 功能分支
- `bugfix/*`: Bug 修复分支

## 部署说明

### Docker 部署

```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f [service-name]
```

### 生产环境配置

修改 `docker-compose.yml` 中的环境变量：

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - MYSQL_HOST=your-mysql-host
  - REDIS_HOST=your-redis-host
```

## 已知问题与待办

详见 `需求&&BUG.txt` 文件

### 近期待办
- [ ] 优化在线人数实时更新
- [ ] 完善消息推送机制
- [ ] 修复个人资料头像上传问题
- [ ] 增强邀请链接安全性

## 许可证

本项目仅供学习和研究使用

## 联系方式

如有问题，请通过以下方式联系：

- Email: [你的邮箱]
- GitHub Issues: [项目 Issue 地址]

---

**最后更新时间**: 2026-03-13
