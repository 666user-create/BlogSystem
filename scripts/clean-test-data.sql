-- ============================================================
-- 测试数据清理脚本（安全版）
--
-- ⚠️ 变更说明：旧版本用「保留 admin + 最近 2 个账号，其余全删」的逻辑，
--    会误删项目拥有者自己注册的账号和博客（2026-09 已发生过一次误删事故，
--    后通过 MySQL binlog 恢复）。本版本改为「只删测试命名模式的账号」。
--
-- 识别规则：自动化测试的账号名 = 字母前缀 + 6 位以上数字（时间戳），
--    例如 userA1786728691625、userB1786728691694、nor1786728691994、py7870309。
--    真实账号（zhangsan / lisi / licai / yudon / admin / admin123）不含数字后缀，不会被匹配。
--
-- 使用建议：
--    1. 先执行「第 1 步」的 SELECT，确认待删列表符合预期，再执行后面的 DELETE；
--    2. 执行前先备份：mysqldump -uroot -proot java_blog_spring > backup.sql
-- ============================================================

USE java_blog_spring;

-- ============ 第 1 步：先看要删什么（务必确认后再往下执行）============
SELECT id, user_name, create_time
FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{6,}$'
ORDER BY id;

SELECT COUNT(*) AS '待删测试账号数'
FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{6,}$';

SELECT COUNT(*) AS '待删测试博客数'
FROM blog_info
WHERE user_id IN (SELECT id FROM user_info WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{6,}$');

-- ============ 第 2 步：删除测试账号的博客与账号 ============
DELETE FROM blog_info
WHERE user_id IN (SELECT id FROM (SELECT id FROM user_info
                                 WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{6,}$') t);

DELETE FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{6,}$';

-- ============ 第 3 步：确认结果 ============
SELECT (SELECT COUNT(*) FROM user_info)                          AS '剩余账号数',
       (SELECT COUNT(*) FROM blog_info)                          AS '剩余博客数',
       (SELECT COUNT(*) FROM blog_info
        WHERE delete_flag = 0 AND published_status = 1)          AS '可见博客数';

SELECT id, user_name FROM user_info ORDER BY id;
