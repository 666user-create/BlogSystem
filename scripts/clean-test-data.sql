-- ============================================================
-- 测试数据清理脚本
-- 用途：自动化测试（UI / 接口）会不断注册随机用户并发表博客，
--      由于列表接口【无分页】（缺陷 BUG-06），数据累积会显著拖慢
--      UI 测试与页面加载。长期跑测试前可执行本脚本清理。
--
-- 说明：
--   1. 采用逻辑删除（delete_flag=1），保留数据可追溯，不影响外键关系；
--   2. 只清理"非 admin 账号"发表的博客——测试账号均为随机用户名，
--      真实使用中如有人工注册账号，请自行调整 WHERE 条件；
--   3. 用户表不做删除（测试账号本身不影响列表接口性能）。
--
-- 执行方式（任选）：
--   mysql -uroot -proot java_blog_spring < scripts/clean-test-data.sql
--   或在 IDEA / Navicat 等客户端中打开执行
-- ============================================================

USE java_blog_spring;

-- 清理前：查看待清理数量
SELECT COUNT(*) AS '清理前-上架未删除博客数'
FROM blog_info b
         JOIN user_info u ON b.user_id = u.id
WHERE b.delete_flag = 0
  AND u.user_name <> 'admin';

-- 逻辑删除所有非 admin 账号发布的博客
UPDATE blog_info b
    JOIN user_info u ON b.user_id = u.id
SET b.delete_flag  = 1,
    b.update_time  = NOW()
WHERE b.delete_flag = 0
  AND u.user_name <> 'admin';

-- 清理后：确认结果
SELECT COUNT(*) AS '清理后-上架未删除博客数'
FROM blog_info
WHERE delete_flag = 0
  AND published_status = 1;
