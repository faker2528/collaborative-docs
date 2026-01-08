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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_document_id (document_id),
    INDEX idx_token (token),
    INDEX idx_creator_id (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分享链接表';

-- 插入测试用户 (密码: 123456, BCrypt加密)
INSERT INTO t_user (id, username, password, email, nickname, status, deleted)
VALUES 
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '管理员', 1, 0),
(2, 'test', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'test@example.com', '测试用户', 1, 0);

