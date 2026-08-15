-- ============================================
-- blog-cloud 微服务学习工程 建库脚本
-- 数据库: blog_cloud
-- 说明: 用户表/博客表结构沿用单体工程 init.sql,
--       额外加了 blog_count(供 Seata/RocketMQ 演示)和 undo_log(Seata 回滚日志表)
-- ============================================

CREATE DATABASE IF NOT EXISTS blog_cloud
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE blog_cloud;

-- ============================================
-- 用户表(由 blog-user-service 管理)
-- ============================================
DROP TABLE IF EXISTS user_info;
CREATE TABLE user_info (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_name   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(32位盐+32位MD5)',
    github_url  VARCHAR(200) DEFAULT NULL COMMENT 'GitHub主页',
    blog_count  INT          DEFAULT 0  COMMENT '博客数(冗余字段, 用于Seata/RocketMQ演示)',
    delete_flag TINYINT      DEFAULT 0  COMMENT '删除标记:0正常 1已删除',
    create_time DATE         DEFAULT NULL COMMENT '创建日期',
    update_time DATE         DEFAULT NULL COMMENT '更新日期'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ============================================
-- 博客表(由 blog-blog-service 管理)
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
-- Seata 分布式事务回滚日志表(每个业务库都必须建这张表)
-- ============================================
DROP TABLE IF EXISTS undo_log;
CREATE TABLE undo_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'increment id',
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(100) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';

-- ============================================
-- 管理员账号(应用启动时自动创建 admin/admin123, 无需手动插入)
-- ============================================
