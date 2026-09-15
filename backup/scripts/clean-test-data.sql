-- ============================================================
-- 测试数据清理脚本（安全版）
--
-- 背景：自动化用例每次执行都会注册新账号、发新博客，长期累积会让
--      列表页变慢、也干扰手工验证，所以跑完自动化建议清一次。
--
-- ⚠️ 历史教训（2026-09）：旧版本用「保留 admin + 最近 2 个账号，其余全删」的逻辑，
--    误删了项目拥有者自己注册的账号和博客，最后靠 MySQL binlog 才恢复
--    （恢复工具见 scripts/restore-from-binlog.py）。所以现在改成
--    「只删符合测试命名规则的账号」，其余一律不动。
--
-- 识别规则：自动化测试账号 = 字母前缀 + 数字后缀
--    UI 用例（BlogUiTest）：前缀 + 6 位固定后缀，如 pager436160、del005794
--    pytest 用例（conftest）：前缀 + 7~8 位时间戳，如 userA1786728691625
--    这里用 {4,} 而不是 {6,}：历史遗留账号的后缀位数不固定（例如 del5794 只有 4 位），
--    放宽下界才能把它们一起清掉；真实账号（zhangsan / lisi / admin / admin123 /
--    licai / yudon）最多 3 位数字，不会被匹配。
--
-- 使用建议：
--    1. 先执行「第 1 步」的 SELECT，确认待删清单符合预期，再往下执行；
--    2. 执行前先备份：mysqldump -uroot -proot java_blog_spring > backup.sql
-- ============================================================

USE java_blog_spring;

-- ============ 第 1 步：先看要删什么（务必确认后再往下执行）============
SELECT id, user_name, create_time
FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{4,}$'
ORDER BY id;

SELECT COUNT(*) AS '待删测试账号数'
FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{4,}$';

SELECT COUNT(*) AS '待删测试博客数'
FROM blog_info
WHERE user_id IN (SELECT id FROM user_info WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{4,}$');

-- ============ 第 2 步：删除测试账号的博客与账号 ============
DELETE FROM blog_info
WHERE user_id IN (SELECT id FROM (SELECT id FROM user_info
                                 WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{4,}$') t);

DELETE FROM user_info
WHERE user_name REGEXP '^[a-zA-Z]+[0-9]{4,}$';

-- ============ 第 3 步：确认结果 ============
SELECT (SELECT COUNT(*) FROM user_info)                          AS '剩余账号数',
       (SELECT COUNT(*) FROM blog_info)                          AS '剩余博客数',
       (SELECT COUNT(*) FROM blog_info
        WHERE delete_flag = 0 AND published_status = 1)          AS '可见博客数';

SELECT id, user_name FROM user_info ORDER BY id;
