-- 更新 t_share_link 表结构以支持验证功能

-- 添加验证类型字段
ALTER TABLE t_share_link 
ADD COLUMN verification_type TINYINT DEFAULT 0 COMMENT '验证类型: 0-免验证, 1-密码验证, 2-邮箱验证';

-- 添加密码字段
ALTER TABLE t_share_link 
ADD COLUMN password VARCHAR(255) COMMENT '访问密码（加密存储）';

-- 添加邮箱字段
ALTER TABLE t_share_link 
ADD COLUMN email VARCHAR(100) COMMENT '指定邮箱（当verificationType为2时使用）';

-- 添加验证类型索引
ALTER TABLE t_share_link 
ADD INDEX idx_verification_type (verification_type);

-- 显示表结构确认更新
DESCRIBE t_share_link;