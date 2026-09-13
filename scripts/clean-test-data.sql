-- ============================================================
-- 测试数据清理脚本
-- 用途：自动化测试（UI / 接口）会不断注册随机用户并发表博客。
--      由于列表接口【无分页】（缺陷 BUG-06），数据累积会显著拖慢
--      UI 测试与页面加载（实测：76 篇博客时 UI 测试从 50 秒劣化到 325 秒）。
--
-- 清理策略：只保留 admin + 最近注册的 2 个账号（及其博客），其余全部删除。
--          测试用例都是"自建数据"，清空后重新执行测试即可。
--
-- 说明：
--   1. 采用**物理删除**（测试数据无需留存），比逻辑删除更彻底；
--   2. 保留数量可调整：修改下面两处 `LIMIT 2` 的数字即可；如只保留 admin，改为 `LIMIT 0`；
--   3. 执行前建议先备份（如 `mysqldump java_blog_spring > backup.sql`）。
--
-- 执行方式（任选）：
--   mysql -uroot -proot java_blog_spring < scripts/clean-test-data.sql
--   或在 IDEA / Navicat 等客户端中打开执行
-- ============================================================

USE java_blog_spring;

-- 清理前：查看当前数据量
SELECT (SELECT COUNT(*) FROM user_info)                            AS '清理前-用户数',
       (SELECT COUNT(*) FROM blog_info)                            AS '清理前-博客数',
       (SELECT COUNT(*) FROM blog_info
        WHERE delete_flag = 0 AND published_status = 1)            AS '清理前-上架博客数';

-- 1. 删除"不保留账号"的全部博客
DELETE b
FROM blog_info b
         JOIN user_info u ON b.user_id = u.id
WHERE u.user_name <> 'admin'
  AND u.id NOT IN (SELECT id FROM (SELECT id FROM user_info
                                   WHERE user_name <> 'admin'
                                   ORDER BY id DESC LIMIT 2) keep_user);

-- 2. 删除"不保留账号"本身
DELETE
FROM user_info
WHERE user_name <> 'admin'
  AND id NOT IN (SELECT id FROM (SELECT id FROM user_info
                                 WHERE user_name <> 'admin'
                                 ORDER BY id DESC LIMIT 2) keep_user2);

-- 清理后：确认结果（应只剩 admin + 最多 2 个账号）
SELECT (SELECT COUNT(*) FROM user_info)                            AS '清理后-用户数',
       (SELECT COUNT(*) FROM blog_info)                            AS '清理后-博客数',
       (SELECT COUNT(*) FROM blog_info
        WHERE delete_flag = 0 AND published_status = 1)            AS '清理后-上架博客数';

SELECT id, user_name, blog_count
FROM user_info
ORDER BY id;
