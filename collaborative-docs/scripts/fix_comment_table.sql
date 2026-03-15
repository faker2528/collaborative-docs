-- 修复评论表和评论点赞表的自增ID
-- 如果表已存在但没有 AUTO_INCREMENT，执行以下语句

-- 方法1：删除并重建表（推荐，如果表中没有重要数据）
-- DROP TABLE IF EXISTS t_comment_like;
-- DROP TABLE IF EXISTS t_comment;

-- 方法2：修改现有表结构（保留数据）
-- 注意：如果表不存在会报错，请忽略错误

-- 修改评论表
ALTER TABLE t_comment MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID';

-- 添加 reply_to_id 字段（如果不存在）
-- MySQL 不支持 IF NOT EXISTS，使用存储过程实现
DELIMITER //
CREATE PROCEDURE add_reply_to_id_if_not_exists()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO column_exists 
    FROM information_schema.columns 
    WHERE table_schema = DATABASE() 
      AND table_name = 't_comment' 
      AND column_name = 'reply_to_id';
    
    IF column_exists = 0 THEN
        ALTER TABLE t_comment ADD COLUMN reply_to_id BIGINT COMMENT '回复的目标评论ID' AFTER parent_id;
        ALTER TABLE t_comment ADD INDEX idx_reply_to_id (reply_to_id);
    END IF;
END //
DELIMITER ;

CALL add_reply_to_id_if_not_exists();
DROP PROCEDURE IF EXISTS add_reply_to_id_if_not_exists;

-- 修改评论点赞表
ALTER TABLE t_comment_like MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID';

-- 验证修改结果
SHOW CREATE TABLE t_comment;
SHOW CREATE TABLE t_comment_like;
