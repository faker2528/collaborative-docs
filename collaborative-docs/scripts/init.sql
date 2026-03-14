-- 创建Nacos配置数据库
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用业务数据库
USE collab_docs;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（加密）',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    bio VARCHAR(500) COMMENT '个人简介',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 文档表
CREATE TABLE IF NOT EXISTS t_document (
    id BIGINT PRIMARY KEY COMMENT '文档ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content LONGTEXT COMMENT '文档内容（富文本JSON）',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    version INT DEFAULT 1 COMMENT '当前版本号',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-草稿, 1-已发布, 2-已归档',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_creator_id (creator_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- 文档权限表
CREATE TABLE IF NOT EXISTS t_document_permission (
    id BIGINT PRIMARY KEY COMMENT '权限ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    permission_type TINYINT NOT NULL COMMENT '权限类型: 1-查看, 2-编辑, 3-管理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_doc_user (document_id, user_id),
    INDEX idx_document_id (document_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档权限表';

-- 文档版本历史表
CREATE TABLE IF NOT EXISTS t_document_history (
    id BIGINT PRIMARY KEY COMMENT '历史ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version INT NOT NULL COMMENT '版本号',
    content LONGTEXT COMMENT '文档内容快照',
    operation_type TINYINT NOT NULL COMMENT '操作类型: 1-创建, 2-编辑, 3-回滚',
    operation_desc VARCHAR(200) COMMENT '操作描述',
    operator_id BIGINT NOT NULL COMMENT '操作者ID',
    operator_name VARCHAR(50) COMMENT '操作者名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_document_id (document_id),
    INDEX idx_version (document_id, version),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档版本历史表';

-- 好友关系表
CREATE TABLE IF NOT EXISTS t_friendship (
    id BIGINT PRIMARY KEY COMMENT '关系 ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    friend_id BIGINT NOT NULL COMMENT '好友ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已删除, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_friend (user_id, friend_id),
    INDEX idx_user_id (user_id),
    INDEX idx_friend_id (friend_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

-- 好友请求表
CREATE TABLE IF NOT EXISTS t_friend_request (
    id BIGINT PRIMARY KEY COMMENT '请求ID',
    from_user_id BIGINT NOT NULL COMMENT '发送者ID',
    to_user_id BIGINT NOT NULL COMMENT '接收者ID',
    message VARCHAR(200) COMMENT '附加消息',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待处理, 1-已同意, 2-已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_from_user (from_user_id),
    INDEX idx_to_user (to_user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友请求表';

-- 文档分享链接表
CREATE TABLE IF NOT EXISTS t_share_link (
    id BIGINT PRIMARY KEY COMMENT '链接ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    creator_id BIGINT NOT NULL COMMENT '创建者ID',
    token VARCHAR(64) NOT NULL UNIQUE COMMENT '分享令牌',
    permission_type TINYINT DEFAULT 1 COMMENT '权限类型: 1-查看, 2-编辑',
    expire_time DATETIME COMMENT '过期时间(空表示永不过期)',
    max_uses INT DEFAULT 0 COMMENT '最大使用次数(0表示无限制)',
    used_count INT DEFAULT 0 COMMENT '已使用次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已禁用, 1-正常',
    verification_type TINYINT DEFAULT 0 COMMENT '验证类型: 0-免验证, 1-密码验证, 2-邮箱验证',
    password VARCHAR(255) COMMENT '访问密码（加密存储）',
    email VARCHAR(100) COMMENT '指定邮箱（当verificationType为2时使用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_document_id (document_id),
    INDEX idx_token (token),
    INDEX idx_creator_id (creator_id),
    INDEX idx_verification_type (verification_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分享链接表';

-- 会话表
CREATE TABLE IF NOT EXISTS t_conversation (
    id BIGINT PRIMARY KEY COMMENT '会话ID',
    type TINYINT NOT NULL COMMENT '会话类型: 1-好友私聊, 2-陌生人私聊, 3-系统消息, 4-申请消息',
    user_id BIGINT NOT NULL COMMENT '用户ID（会话所属用户）',
    target_user_id BIGINT COMMENT '对方用户ID（私聊时）',
    title VARCHAR(100) COMMENT '会话标题（系统消息时使用）',
    last_message_id BIGINT COMMENT '最后一条消息ID',
    last_message_content VARCHAR(200) COMMENT '最后一条消息内容摘要',
    last_message_time DATETIME COMMENT '最后消息时间',
    unread_count INT DEFAULT 0 COMMENT '未读消息数',
    pinned TINYINT DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    muted TINYINT DEFAULT 0 COMMENT '是否静音: 0-否, 1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_user_id (user_id),
    INDEX idx_target_user_id (target_user_id),
    INDEX idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS t_message (
    id BIGINT PRIMARY KEY COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    type TINYINT DEFAULT 1 COMMENT '消息类型: 1-文字, 2-图片, 3-文件, 4-系统通知',
    content TEXT COMMENT '消息内容',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    attachment_name VARCHAR(200) COMMENT '附件名称',
    attachment_size BIGINT COMMENT '附件大小(字节)',
    status TINYINT DEFAULT 0 COMMENT '消息状态: 0-发送中, 1-已送达, 2-已读',
    extra_data JSON COMMENT '额外数据（申请类消息的额外信息）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- 系统通知表
CREATE TABLE IF NOT EXISTS t_system_notification (
    id BIGINT PRIMARY KEY COMMENT '通知ID',
    type TINYINT NOT NULL COMMENT '通知类型: 1-平台通知, 2-功能更新, 3-文档操作提醒, 4-权限变更',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    receiver_id BIGINT COMMENT '接收者ID（null表示全体用户）',
    ref_type VARCHAR(50) COMMENT '关联业务类型',
    ref_id BIGINT COMMENT '关联业务ID',
    link VARCHAR(500) COMMENT '跳转链接',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 文件记录表
CREATE TABLE IF NOT EXISTS t_file_record (
    id BIGINT PRIMARY KEY COMMENT '文件ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型(MIME)',
    file_ext VARCHAR(20) COMMENT '文件扩展名',
    storage_type TINYINT DEFAULT 1 COMMENT '存储类型: 1-本地, 2-MinIO, 3-OSS',
    bucket_name VARCHAR(100) COMMENT '存储桶名称',
    uploader_id BIGINT NOT NULL COMMENT '上传者ID',
    business_type VARCHAR(50) COMMENT '业务类型: chat-聊天, document-文档, avatar-头像',
    business_id BIGINT COMMENT '关联业务ID',
    download_count INT DEFAULT 0 COMMENT '下载次数',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已删除, 1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除: 0-否, 1-是',
    INDEX idx_uploader_id (uploader_id),
    INDEX idx_business (business_type, business_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件记录表';

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO t_user (id, username, password, email, nickname, status, deleted)
VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '管理员', 1, 0),
(2, 'test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'test@example.com', '测试用户', 1, 0);

