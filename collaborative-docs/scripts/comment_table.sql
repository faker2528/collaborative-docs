-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
    `document_id` BIGINT(20) NOT NULL COMMENT '文档 ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户 ID',
    `text` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT(20) DEFAULT NULL COMMENT '父评论 ID（回复）',
    `like_count` INT(11) DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '是否已删除 (0:未删除 1:已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_document_id` (`document_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `comment_id` BIGINT(20) NOT NULL COMMENT '评论 ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户 ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

ALTER TABLE t_comment ADD COLUMN reply_to_id BIGINT COMMENT '回复的目标评论ID' AFTER parent_id;
ALTER TABLE t_comment ADD INDEX idx_reply_to_id (reply_to_id);