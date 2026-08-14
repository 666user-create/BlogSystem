-- ============================================
-- BlogSystem 建库脚本
-- 数据库: java_blog_spring
-- ============================================

CREATE DATABASE IF NOT EXISTS java_blog_spring
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE java_blog_spring;

-- ============================================
-- 用户表
-- ============================================
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_name   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(32位盐+32位MD5)',
    github_url  VARCHAR(200) DEFAULT NULL COMMENT 'GitHub主页',
    delete_flag TINYINT      DEFAULT 0  COMMENT '删除标记:0正常 1已删除',
    create_time DATE         DEFAULT NULL COMMENT '创建日期',
    update_time DATE         DEFAULT NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ============================================
-- 博客表
-- ============================================
DROP TABLE IF EXISTS blog_info;
CREATE TABLE blog_info (
    id               INT AUTO_INCREMENT PRIMARY KEY COMMENT '博客ID',
    title            VARCHAR(200) NOT NULL COMMENT '博客标题',
    content          TEXT         NOT NULL COMMENT '博客内容(Markdown)',
    user_id          VARCHAR(20)  NOT NULL COMMENT '作者ID(对应user_info.id)',
    published_status TINYINT      DEFAULT 1  COMMENT '上下架:1上架 0下架',
    delete_flag      TINYINT      DEFAULT 0  COMMENT '删除标记:0正常 1已删除',
    create_time      DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time      DATETIME     DEFAULT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='博客信息表';

-- ============================================
-- 管理员账号（应用启动时也会自动创建）
-- 密码 admin123 经 SecurityUtil.encrypt() 哈希
-- ============================================
-- INSERT INTO user_info (user_name, password, github_url, delete_flag, create_time, update_time)
-- VALUES ('admin', '<32位随机盐+32位MD5>', 'https://github.com', 0, CURDATE(), CURDATE());
-- 注：启动项目后 BlogSystemApplication 会自动创建 admin/admin123，无需手动执行上面这条
