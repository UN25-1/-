-- ================================================================
-- 一键初始化脚本: 建表 + 导入数据 + 创建只读用户
-- 使用方式: mysql -u root -p < init_food_db_full.sql
-- ================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS food
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE food;

-- ================================================================
-- 建表部分
-- ================================================================

DROP TABLE IF EXISTS `rider_reviews`;
DROP TABLE IF EXISTS `merchant_reviews`;
DROP TABLE IF EXISTS `order_status_logs`;
DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `payments`;
DROP TABLE IF EXISTS `cart_items`;
DROP TABLE IF EXISTS `user_addresses`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `product_categories`;
DROP TABLE IF EXISTS `rider_details`;
DROP TABLE IF EXISTS `merchant_details`;
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
    `id`         INT             NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`   VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    `password`   VARCHAR(255)    NOT NULL                 COMMENT '密码（bcrypt哈希）',
    `phone`      VARCHAR(20)     NOT NULL                 COMMENT '手机号',
    `role`       VARCHAR(20)     NOT NULL DEFAULT 'user'  COMMENT '角色: user/rider/merchant/admin',
    `status`     TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '状态: 1启用 0禁用',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `merchant_details` (
    `id`               INT             NOT NULL AUTO_INCREMENT  COMMENT '商家ID',
    `user_id`          INT             NOT NULL                 COMMENT '关联用户ID',
    `shop_name`        VARCHAR(100)    NOT NULL                 COMMENT '店铺名称',
    `shop_address`     VARCHAR(255)    NOT NULL                 COMMENT '店铺地址',
    `shop_phone`       VARCHAR(20)     NOT NULL                 COMMENT '店铺电话',
    `description`      TEXT                                     COMMENT '店铺描述',
    `logo_url`         VARCHAR(255)                             COMMENT '店铺Logo URL',
    `opening_time`     TIME            NOT NULL                 COMMENT '营业开始时间',
    `closing_time`     TIME            NOT NULL                 COMMENT '营业结束时间',
    `delivery_fee`     DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '配送费',
    `min_order_amount` DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '最低起送价',
    `rating`           DECIMAL(3,2)    NOT NULL DEFAULT 5.00    COMMENT '评分',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `enabled`          TINYINT(1)      NOT NULL DEFAULT 1       COMMENT '是否启用',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家详情表';

CREATE TABLE `product_categories` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `merchant_id` INT          NOT NULL                COMMENT '商家ID',
    `name`        VARCHAR(50)  NOT NULL                COMMENT '分类名称',
    `sort_order`  INT          NOT NULL DEFAULT 0      COMMENT '排序',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品分类表';

CREATE TABLE `products` (
    `id`           INT             NOT NULL AUTO_INCREMENT COMMENT '产品ID',
    `merchant_id`  INT             NOT NULL                COMMENT '商家ID',
    `category_id`  INT             NOT NULL                COMMENT '分类ID',
    `name`         VARCHAR(100)    NOT NULL                COMMENT '产品名称',
    `description`  VARCHAR(255)                            COMMENT '产品描述',
    `price`        DECIMAL(10,2)   NOT NULL                COMMENT '价格',
    `image_url`    VARCHAR(255)                            COMMENT '图片URL',
    `is_available` TINYINT(1)      NOT NULL DEFAULT 1      COMMENT '是否上架',
    `stock`        INT             NOT NULL DEFAULT 0      COMMENT '库存',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

CREATE TABLE `orders` (
    `id`               INT             NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id`          INT             NOT NULL                COMMENT '用户ID',
    `merchant_id`      INT             NOT NULL                COMMENT '商家ID',
    `rider_id`         INT                                     COMMENT '骑手ID',
    `order_status`     VARCHAR(30)     NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    `total_amount`     DECIMAL(10,2)   NOT NULL                COMMENT '总金额',
    `delivery_fee`     DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '配送费',
    `delivery_address` VARCHAR(255)    NOT NULL                COMMENT '配送地址',
    `contact_phone`    VARCHAR(20)     NOT NULL                COMMENT '联系电话',
    `contact_name`     VARCHAR(50)     NOT NULL                COMMENT '联系人',
    `note`             TEXT                                    COMMENT '备注',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version`          INT             NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_rider_id` (`rider_id`),
    KEY `idx_order_status` (`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE `order_items` (
    `id`         INT           NOT NULL AUTO_INCREMENT COMMENT '订单项ID',
    `order_id`   INT           NOT NULL                COMMENT '订单ID',
    `product_id` INT           NOT NULL                COMMENT '产品ID',
    `quantity`   INT           NOT NULL DEFAULT 1      COMMENT '数量',
    `price`      DECIMAL(10,2) NOT NULL                COMMENT '单价',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单项表';

CREATE TABLE `payments` (
    `id`             INT           NOT NULL AUTO_INCREMENT COMMENT '支付ID',
    `order_id`       INT           NOT NULL                COMMENT '订单ID',
    `pay_method`     VARCHAR(20)   NOT NULL                COMMENT '支付方式',
    `pay_status`     VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT '支付状态',
    `transaction_no` VARCHAR(100)                          COMMENT '交易流水号',
    `paid_amount`    DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    `paid_at`        DATETIME                             COMMENT '支付时间',
    `refunded_at`    DATETIME                             COMMENT '退款时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

CREATE TABLE `order_status_logs` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id`    INT          NOT NULL                COMMENT '订单ID',
    `from_status` VARCHAR(30)                          COMMENT '原状态',
    `to_status`   VARCHAR(30)  NOT NULL                COMMENT '新状态',
    `operator_id` INT                                 COMMENT '操作人ID',
    `remark`      TEXT                                COMMENT '备注',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态日志表';

CREATE TABLE `cart_items` (
    `id`         INT      NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`    INT      NOT NULL                COMMENT '用户ID',
    `product_id` INT      NOT NULL                COMMENT '产品ID',
    `quantity`   INT      NOT NULL DEFAULT 1      COMMENT '数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车项表';

CREATE TABLE `user_addresses` (
    `id`           INT          NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`      INT          NOT NULL                COMMENT '用户ID',
    `contact_name` VARCHAR(50)  NOT NULL                COMMENT '联系人',
    `phone`        VARCHAR(20)  NOT NULL                COMMENT '联系电话',
    `address`      VARCHAR(255) NOT NULL                COMMENT '地址',
    `is_default`   TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否默认地址',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地址表';

CREATE TABLE `rider_details` (
    `id`               INT          NOT NULL AUTO_INCREMENT COMMENT '骑手详情ID',
    `user_id`          INT          NOT NULL                COMMENT '关联用户ID',
    `real_name`        VARCHAR(50)                          COMMENT '真实姓名',
    `id_card`          VARCHAR(18)                          COMMENT '身份证号',
    `vehicle`          VARCHAR(50)                          COMMENT '交通工具',
    `vehicle_number`   VARCHAR(20)                          COMMENT '车牌号',
    `status`           VARCHAR(20)  NOT NULL DEFAULT 'offline' COMMENT '状态',
    `rating`           DECIMAL(3,2) NOT NULL DEFAULT 5.00   COMMENT '评分',
    `completed_orders` INT          NOT NULL DEFAULT 0      COMMENT '完成订单数',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `enabled`          TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否启用',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手详情表';

CREATE TABLE `merchant_reviews` (
    `id`         INT          NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`   INT          NOT NULL                COMMENT '订单ID',
    `user_id`    INT          NOT NULL                COMMENT '用户ID',
    `rating`     TINYINT      NOT NULL                COMMENT '评分',
    `comment`    TEXT                                 COMMENT '评价内容',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家评价表';

CREATE TABLE `rider_reviews` (
    `id`         INT          NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`   INT          NOT NULL                COMMENT '订单ID',
    `user_id`    INT          NOT NULL                COMMENT '用户ID',
    `rider_id`   INT          NOT NULL                COMMENT '骑手ID',
    `rating`     TINYINT      NOT NULL                COMMENT '评分',
    `comment`    TEXT                                 COMMENT '评价内容',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_rider_id` (`rider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑手评价表';

-- ================================================================
-- 数据导入部分（从原始 SQL 文件合并）
-- ================================================================

INSERT INTO `users` (`id`, `username`, `password`, `phone`, `role`, `status`, `created_at`)
VALUES  (1, 'zhangsan', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13800001111', 'user', 1, '2026-06-11 10:44:39'),
        (2, 'lisi', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13800002222', 'user', 1, '2026-06-11 10:44:39'),
        (3, 'wangwu', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13800003333', 'user', 1, '2026-06-11 10:44:39'),
        (4, 'rider01', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13900001111', 'rider', 1, '2026-06-11 10:44:39'),
        (5, 'rider02', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13900002222', 'rider', 1, '2026-06-11 10:44:39'),
        (6, 'merchant01', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13700001111', 'merchant', 1, '2026-06-11 10:44:39'),
        (7, 'merchant02', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13700002222', 'merchant', 1, '2026-06-11 10:44:39'),
        (8, 'admin01', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '13600001111', 'admin', 1, '2026-06-11 10:44:39'),
        (9, '张三我', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '11111111111', 'user', 1, '2026-06-11 11:08:45'),
        (10, '管理员', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '22222222222', 'admin', 1, '2026-06-11 11:09:59'),
        (11, '扎根三', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '11111111111', 'user', 1, '2026-06-11 18:59:53'),
        (12, '骑手1', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '18970818750', 'rider', 1, '2026-06-11 19:14:15'),
        (13, '商家1', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '11111111111', 'merchant', 1, '2026-06-11 20:21:39');

INSERT INTO `merchant_details` (`id`, `user_id`, `shop_name`, `shop_address`, `shop_phone`, `description`, `logo_url`, `opening_time`, `closing_time`, `delivery_fee`, `min_order_amount`, `rating`, `created_at`, `enabled`)
VALUES  (1, 6, '美味中餐馆', '长沙市岳麓区麓谷大道100号', '13700001111', '主营正宗湘菜，味道鲜美', NULL, '09:00:00', '21:00:00', 5.00, 20.00, 5.00, '2026-06-11 10:44:39', 1),
        (2, 7, '西式快餐店', '长沙市芙蓉区五一大道200号', '13700002222', '汉堡、薯条、炸鸡，快速送达', NULL, '10:00:00', '23:00:00', 3.00, 15.00, 5.00, '2026-06-11 10:44:39', 1);

INSERT INTO `product_categories` (`id`, `merchant_id`, `name`, `sort_order`, `created_at`)
VALUES  (1, 1, '热菜', 1, '2026-06-11 10:44:39'),
        (2, 1, '素菜', 2, '2026-06-11 10:44:39'),
        (3, 1, '主食', 3, '2026-06-11 10:44:39'),
        (4, 2, '汉堡', 1, '2026-06-11 10:44:39'),
        (5, 2, '小食', 2, '2026-06-11 10:44:39'),
        (6, 2, '饮品', 3, '2026-06-11 10:44:39');

INSERT INTO `products` (`id`, `merchant_id`, `category_id`, `name`, `description`, `price`, `image_url`, `is_available`, `stock`, `created_at`)
VALUES  (1, 1, 1, '辣椒炒肉', '经典湘菜，香辣下饭', 28.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (2, 1, 1, '剁椒鱼头', '新鲜鱼头配剁椒，鲜辣可口', 58.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (3, 1, 2, '蒜蓉空心菜', '时令蔬菜，清淡爽口', 18.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (4, 1, 3, '米饭', '东北大米', 2.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (5, 2, 4, '香辣鸡腿堡', '大块鸡腿肉，香辣酱汁', 18.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (6, 2, 5, '薯条（大）', '外酥里嫩，黄金薯条', 12.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (7, 2, 6, '可乐', '冰爽可乐', 5.00, NULL, 1, 9999, '2026-06-11 10:44:39'),
        (8, 2, 5, '鸡米花', '一口一个，酥脆可口', 10.00, NULL, 1, 9999, '2026-06-11 10:44:39');

INSERT INTO `orders` (`id`, `user_id`, `merchant_id`, `rider_id`, `order_status`, `total_amount`, `delivery_fee`, `delivery_address`, `contact_phone`, `contact_name`, `note`, `created_at`, `updated_at`, `version`)
VALUES  (1, 1, 1, 1, 'delivered', 106.00, 5.00, '长沙岳麓区麓谷小镇3栋501', '13800001111', '张三', '不要放葱', '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (2, 2, 2, NULL, 'pending', 35.00, 3.00, '长沙芙蓉区解放路100号', '13800002222', '李四', '多加点番茄酱', '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (3, 3, 1, NULL, 'pending_payment', 48.00, 5.00, '长沙岳麓区中联科技园', '13800003333', '王五', NULL, '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (4, 1, 2, 1, 'delivering', 30.00, 3.00, '长沙岳麓区麓谷小镇3栋501', '13800001111', '张三', '快点送达', '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (5, 2, 1, 2, 'cancelled', 78.00, 5.00, '长沙芙蓉区解放路100号', '13800002222', '李四', '临时取消', '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (6, 1, 2, 1, 'completed', 35.00, 3.00, '长沙岳麓区麓谷小镇3栋501', '13800001111', '张三', '已确认收货', '2026-06-11 10:44:39', '2026-06-11 10:44:39', 0),
        (7, 11, 1, NULL, 'preparing', 33.00, 5.00, '认为是地方', '11111111111', '十大', NULL, '2026-06-11 19:11:48', '2026-06-13 12:55:10', 0);

INSERT INTO `order_items` (`id`, `order_id`, `product_id`, `quantity`, `price`)
VALUES  (1, 1, 1, 1, 28.00), (2, 1, 2, 1, 58.00), (3, 1, 4, 2, 2.00),
        (4, 2, 5, 1, 18.00), (5, 2, 6, 1, 12.00), (6, 2, 7, 1, 5.00),
        (7, 3, 1, 1, 28.00), (8, 3, 3, 1, 18.00), (9, 3, 4, 1, 2.00),
        (10, 4, 5, 1, 18.00), (11, 4, 8, 1, 10.00), (12, 4, 7, 1, 5.00),
        (13, 5, 2, 1, 58.00), (14, 5, 4, 2, 2.00),
        (15, 6, 5, 1, 18.00), (16, 6, 8, 1, 10.00), (17, 6, 7, 1, 5.00),
        (18, 7, 1, 1, 28.00);

INSERT INTO `payments` (`id`, `order_id`, `pay_method`, `pay_status`, `transaction_no`, `paid_amount`, `paid_at`, `refunded_at`)
VALUES  (1, 1, 'wechat', 'success', NULL, 106.00, NULL, NULL),
        (2, 3, 'alipay', 'pending', NULL, 48.00, NULL, NULL),
        (3, 6, 'wechat', 'success', NULL, 35.00, NULL, NULL),
        (4, 7, 'wechat', 'success', 'PAY_WECHAT_1781176311137_5064', 33.00, '2026-06-11 19:11:51', NULL);

INSERT INTO `order_status_logs` (`id`, `order_id`, `from_status`, `to_status`, `operator_id`, `remark`, `created_at`)
VALUES  (1, 1, NULL, 'pending', 1, NULL, '2026-06-11 10:44:39'),
        (2, 1, 'pending', 'preparing', 6, NULL, '2026-06-11 10:44:39'),
        (3, 1, 'preparing', 'delivering', 1, NULL, '2026-06-11 10:44:39'),
        (4, 1, 'delivering', 'delivered', 1, NULL, '2026-06-11 10:44:39'),
        (5, 2, NULL, 'pending', 2, NULL, '2026-06-11 10:44:39'),
        (6, 5, NULL, 'pending', 2, NULL, '2026-06-11 10:44:39'),
        (7, 5, 'pending', 'cancelled', 2, NULL, '2026-06-11 10:44:39'),
        (8, 7, NULL, 'pending_payment', 11, NULL, '2026-06-11 19:11:48'),
        (9, 7, 'pending_payment', 'pending', 11, '微信支付支付成功，交易号：PAY_WECHAT_1781176311137_5064', '2026-06-11 19:11:51'),
        (10, 7, 'pending', 'preparing', 6, NULL, '2026-06-13 12:55:10');

INSERT INTO `cart_items` (`id`, `user_id`, `product_id`, `quantity`, `created_at`)
VALUES  (1, 1, 1, 1, '2026-06-11 10:44:39'),
        (2, 1, 4, 2, '2026-06-11 10:44:39'),
        (3, 10, 1, 2, '2026-06-11 18:43:19'),
        (4, 10, 2, 1, '2026-06-11 18:43:20');

INSERT INTO `user_addresses` (`id`, `user_id`, `contact_name`, `phone`, `address`, `is_default`, `created_at`)
VALUES  (1, 1, '张三', '13800001111', '长沙岳麓区麓谷小镇3栋501', 1, '2026-06-11 10:44:39'),
        (2, 1, '张三公司', '13800001111', '长沙岳麓区中联科技园B座', 0, '2026-06-11 10:44:39'),
        (3, 2, '李四', '13800002222', '长沙芙蓉区解放路100号', 1, '2026-06-11 10:44:39'),
        (4, 3, '王五', '13800003333', '长沙岳麓区中联科技园', 1, '2026-06-11 10:44:39'),
        (5, 11, '十大', '11111111111', '认为是地方', 1, '2026-06-11 19:11:37');

INSERT INTO `rider_details` (`id`, `user_id`, `real_name`, `id_card`, `vehicle`, `vehicle_number`, `status`, `rating`, `completed_orders`, `created_at`, `enabled`)
VALUES  (1, 4, '赵骑手', '430100199001011234', '电动车', '湘A·00001', 'online', 5.00, 0, '2026-06-11 10:44:39', 1),
        (2, 5, '钱骑手', '430100199102022345', '摩托车', '湘A·00002', 'offline', 5.00, 0, '2026-06-11 10:44:39', 1),
        (3, 12, NULL, NULL, NULL, NULL, 'online', 5.00, 0, '2026-06-11 19:14:31', 1);

INSERT INTO `merchant_reviews` (`id`, `order_id`, `user_id`, `rating`, `comment`, `created_at`)
VALUES  (1, 1, 1, 5, '味道很好，送达也快！', '2026-06-11 10:44:39'),
        (2, 5, 2, 3, '临时有事取消了，不是商家问题，整体不错', '2026-06-11 10:44:39');

INSERT INTO `rider_reviews` (`id`, `order_id`, `user_id`, `rider_id`, `rating`, `comment`, `created_at`)
VALUES  (1, 1, 1, 1, 5, '骑手态度好，速度快', '2026-06-11 10:44:39'),
        (2, 6, 1, 1, 4, '这次稍微有点慢，不过还能接受', '2026-06-11 10:44:39');

-- ================================================================
-- 创建只读用户（仅有 SELECT 权限）
-- ================================================================

-- DROP USER IF EXISTS 'food_reader'@'localhost';
-- CREATE USER 'food_reader'@'localhost' IDENTIFIED BY 'FoodRead2024!';
-- GRANT SELECT ON food.* TO 'food_reader'@'localhost';
-- FLUSH PRIVILEGES;

-- ================================================================
-- 初始化完成
-- 登录方式: mysql -u food_reader -p'FoodRead2024!' -D food
-- ================================================================





-- ================================================================
-- 新增商家数据：5个商家账号，每个12个菜品
-- ================================================================

-- ---------- 新增用户账号（4个新商家用户）----------
INSERT INTO `users` (`id`, `username`, `password`, `phone`, `role`, `status`, `created_at`)
VALUES
    (14, 'merchant03', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15600001111', 'merchant', 1, '2026-06-14 09:00:00'),
    (15, 'merchant04', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15600002222', 'merchant', 1, '2026-06-14 09:00:00'),
    (16, 'merchant05', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15600003333', 'merchant', 1, '2026-06-14 09:00:00'),
    (17, 'merchant06', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15600004444', 'merchant', 1, '2026-06-14 09:00:00');

-- ---------- 新增商家详情（5家）----------
INSERT INTO `merchant_details` (`id`, `user_id`, `shop_name`, `shop_address`, `shop_phone`, `description`, `logo_url`, `opening_time`, `closing_time`, `delivery_fee`, `min_order_amount`, `rating`, `created_at`, `enabled`)
VALUES
    (3, 13, '家常私房菜', '长沙市天心区解放西路50号', '11111111111', '地道家常味道，每日新鲜现做，妈妈的味道', NULL, '10:00:00', '20:00:00', 3.00, 10.00, 5.00, '2026-06-14 09:00:00', 1),
    (4, 14, '麦香汉堡', '长沙市开福区芙蓉中路200号', '15600001111', '新鲜现做汉堡，美味快餐，快速送达', NULL, '09:00:00', '22:00:00', 4.00, 15.00, 5.00, '2026-06-14 09:00:00', 1),
    (5, 15, '茶语时光', '长沙市雨花区韶山南路88号', '15600002222', '手作茶饮，鲜果现调，每一杯都是好心情', NULL, '10:00:00', '22:00:00', 3.00, 10.00, 5.00, '2026-06-14 09:00:00', 1),
    (6, 16, '晨光早餐', '长沙市岳麓区桐梓坡路120号', '15600003333', '营养早餐，开启活力满满的一天', NULL, '06:00:00', '11:00:00', 2.00, 8.00, 5.00, '2026-06-14 09:00:00', 1),
    (7, 17, '粥品世家', '长沙市芙蓉区人民路66号', '15600004444', '匠心熬制数小时，百味粥品暖人心', NULL, '06:00:00', '13:00:00', 3.00, 10.00, 5.00, '2026-06-14 09:00:00', 1);

-- ---------- 新增产品分类（每家商家3-4个分类）----------
INSERT INTO `product_categories` (`id`, `merchant_id`, `name`, `sort_order`, `created_at`)
VALUES
    -- 家常私房菜 (merchant_id=3)
    (7,  3, '家常热菜', 1, '2026-06-14 09:00:00'),
    (8,  3, '凉菜',     2, '2026-06-14 09:00:00'),
    (9,  3, '汤品',     3, '2026-06-14 09:00:00'),
    (10, 3, '主食',     4, '2026-06-14 09:00:00'),

    -- 麦香汉堡 (merchant_id=4)
    (11, 4, '经典汉堡', 1, '2026-06-14 09:00:00'),
    (12, 4, '小食配餐', 2, '2026-06-14 09:00:00'),
    (13, 4, '饮品',     3, '2026-06-14 09:00:00'),
    (14, 4, '超值套餐', 4, '2026-06-14 09:00:00'),

    -- 茶语时光 (merchant_id=5)
    (15, 5, '奶茶系列',   1, '2026-06-14 09:00:00'),
    (16, 5, '果茶系列',   2, '2026-06-14 09:00:00'),
    (17, 5, '纯茶与咖啡', 3, '2026-06-14 09:00:00'),

    -- 晨光早餐 (merchant_id=6)
    (18, 6, '面点类', 1, '2026-06-14 09:00:00'),
    (19, 6, '粥粉类', 2, '2026-06-14 09:00:00'),
    (20, 6, '饮品类', 3, '2026-06-14 09:00:00'),

    -- 粥品世家 (merchant_id=7)
    (21, 7, '特色粥品', 1, '2026-06-14 09:00:00'),
    (22, 7, '小菜搭配', 2, '2026-06-14 09:00:00'),
    (23, 7, '面食小吃', 3, '2026-06-14 09:00:00');

-- ---------- 新增产品（每家12个，共60个）----------
INSERT INTO `products` (`id`, `merchant_id`, `category_id`, `name`, `description`, `price`, `image_url`, `is_available`, `stock`, `created_at`)
VALUES
    -- ========== 家常私房菜 (merchant_id=3, products 9-20) ==========
    -- 家常热菜
    (9,  3, 7,  '红烧肉',     '精选五花肉，软糯不腻',               32.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (10, 3, 7,  '番茄炒蛋',   '酸甜可口，下饭神器',                 16.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (11, 3, 7,  '鱼香肉丝',   '酸甜微辣，经典川味',                 26.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (12, 3, 7,  '回锅肉',     '肥而不腻，蒜香浓郁',                 28.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 凉菜
    (13, 3, 8,  '凉拌黄瓜',   '蒜泥调味，清脆爽口',                 10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (14, 3, 8,  '皮蛋豆腐',   '嫩滑豆腐配松花蛋，经典搭配',         12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (15, 3, 8,  '凉拌木耳',   '脆嫩黑木耳，酸辣开胃',               14.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 汤品
    (16, 3, 9,  '紫菜蛋花汤', '清淡鲜美，暖胃首选',                  8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (17, 3, 9,  '西红柿鸡蛋汤','酸甜可口，家常必备',                 10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (18, 3, 9,  '酸辣汤',     '酸辣开胃，料足味浓',                 12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 主食
    (19, 3, 10, '白米饭',     '东北大米，粒粒分明',                  2.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (20, 3, 10, '蛋炒饭',     '粒粒金黄，香气扑鼻',                 10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),

    -- ========== 麦香汉堡 (merchant_id=4, products 21-32) ==========
    -- 经典汉堡
    (21, 4, 11, '香辣鸡腿堡',   '大块鸡腿肉，香辣酱汁',             18.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (22, 4, 11, '牛肉芝士堡',   '厚实牛肉饼，双层芝士',             25.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (23, 4, 11, '双层鳕鱼堡',   '酥脆鳕鱼排，鲜嫩多汁',             22.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 小食配餐
    (24, 4, 12, '薯条（大）',   '外酥里嫩，黄金薯条',               12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (25, 4, 12, '鸡米花',       '一口一个，酥脆可口',               10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (26, 4, 12, '洋葱圈',       '金黄酥脆，洋葱鲜甜',                9.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 饮品
    (27, 4, 13, '可乐（中）',   '冰爽畅快，经典搭配',                7.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (28, 4, 13, '雪碧（中）',   '清爽柠檬味',                        7.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (29, 4, 13, '冰柠檬茶',     '新鲜柠檬，清爽解腻',                8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 超值套餐
    (30, 4, 14, '单人超值套餐', '汉堡+薯条+可乐，一人吃饱',         32.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (31, 4, 14, '双人欢乐套餐', '2汉堡+2小食+2饮品，快乐加倍',      58.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (32, 4, 14, '全家分享桶',   '3汉堡+大薯+鸡米花+洋葱圈+3饮品',   88.00, NULL, 1, 9999, '2026-06-14 09:00:00'),

    -- ========== 茶语时光 (merchant_id=5, products 33-44) ==========
    -- 奶茶系列
    (33, 5, 15, '经典珍珠奶茶', 'Q弹珍珠，浓郁奶香',                 12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (34, 5, 15, '红豆奶茶',     '香甜红豆，暖胃养颜',                 13.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (35, 5, 15, '椰果奶茶',     '爽滑椰果，清新口感',                 12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (36, 5, 15, '芋圆波波奶茶', '软糯芋圆，嚼劲十足',                 16.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 果茶系列
    (37, 5, 16, '满杯百香果',   '鲜切百香果，酸甜爆汁',               15.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (38, 5, 16, '霸气橙子',     '整颗鲜橙，维C满满',                 16.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (39, 5, 16, '多肉葡萄',     '手剥葡萄果肉，茶底清爽',             18.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (40, 5, 16, '芒果多多',     '新鲜芒果，果香浓郁',                 16.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 纯茶与咖啡
    (41, 5, 17, '茉莉绿茶',     '茉莉花香，清雅回甘',                  8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (42, 5, 17, '高山乌龙',     '台湾高山茶，醇厚甘润',                9.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (43, 5, 17, '美式咖啡',     '经典美式，醇苦提神',                 14.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (44, 5, 17, '拿铁咖啡',     '丝滑奶泡，柔和香醇',                 16.00, NULL, 1, 9999, '2026-06-14 09:00:00'),

    -- ========== 晨光早餐 (merchant_id=6, products 45-56) ==========
    -- 面点类
    (45, 6, 18, '鲜肉包（2个）', '皮薄馅大，汁多味美',                5.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (46, 6, 18, '素菜包（2个）', '时蔬馅料，清淡健康',                4.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (47, 6, 18, '刀切馒头',      '松软白面，麦香十足',                2.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (48, 6, 18, '葱油花卷',      '层层分明，葱香四溢',                3.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 粥粉类
    (49, 6, 19, '白粥',          '慢火熬制，米香浓郁',                3.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (50, 6, 19, '皮蛋瘦肉粥',    '皮蛋配瘦肉，经典早餐粥',             8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (51, 6, 19, '鸡蛋肠粉',      '薄如蝉翼，滑嫩爽口',                8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (52, 6, 19, '肉末肠粉',      '肉香四溢，嫩滑饱腹',               10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 饮品类
    (53, 6, 20, '现磨豆浆（甜）', '黄豆现磨，浓郁香甜',               4.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (54, 6, 20, '豆腐脑（咸）',   '嫩滑豆腐脑，配榨菜虾皮',            5.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (55, 6, 20, '纯牛奶',        '250ml盒装纯牛奶',                   5.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (56, 6, 20, '茶叶蛋',        '秘制卤料，入味十足',                2.00, NULL, 1, 9999, '2026-06-14 09:00:00'),

    -- ========== 粥品世家 (merchant_id=7, products 57-68) ==========
    -- 特色粥品
    (57, 7, 21, '红枣桂圆粥',    '红枣桂圆慢熬，补血养气',            10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (58, 7, 21, '山药排骨粥',    '山药配小排，滋补鲜美',              15.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (59, 7, 21, '南瓜小米粥',    '南瓜甜糯，养胃首选',                 8.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (60, 7, 21, '八宝粥',        '八种谷物豆类，营养丰富',             9.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 小菜搭配
    (61, 7, 22, '咸鸭蛋',        '流油起沙，咸香可口',                 3.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (62, 7, 22, '爽口萝卜皮',    '酸甜脆爽，佐粥佳品',                 5.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (63, 7, 22, '酱黄瓜',        '秘制酱料腌制，爽脆开胃',             4.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (64, 7, 22, '香辣海带丝',    '香辣入味，口感爽滑',                 5.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    -- 面食小吃
    (65, 7, 23, '煎饺（6个）',   '底部焦脆，肉馅鲜香',               10.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (66, 7, 23, '小笼包（8个）', '薄皮汤汁，鲜肉内馅',               12.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (67, 7, 23, '葱油饼',        '层层酥脆，葱香扑鼻',                 6.00, NULL, 1, 9999, '2026-06-14 09:00:00'),
    (68, 7, 23, '油条（2根）',   '现炸金黄，外酥里软',                 3.00, NULL, 1, 9999, '2026-06-14 09:00:00');






-- ================================================================
-- 甜品、火锅、烧烤商家（3类×3家=9个账号，每家12个菜品）
-- ================================================================

-- ---------- 新增用户（9个商家账号）----------
INSERT INTO `users` (`id`, `username`, `password`, `phone`, `role`, `status`, `created_at`)
VALUES
    (18, 'dessert01', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000001', 'merchant', 1, '2026-06-14 09:30:00'),
    (19, 'dessert02', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000002', 'merchant', 1, '2026-06-14 09:30:00'),
    (20, 'dessert03', '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000003', 'merchant', 1, '2026-06-14 09:30:00'),
    (21, 'hotpot01',  '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000004', 'merchant', 1, '2026-06-14 09:30:00'),
    (22, 'hotpot02',  '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000005', 'merchant', 1, '2026-06-14 09:30:00'),
    (23, 'hotpot03',  '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000006', 'merchant', 1, '2026-06-14 09:30:00'),
    (24, 'bbq01',     '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000007', 'merchant', 1, '2026-06-14 09:30:00'),
    (25, 'bbq02',     '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000008', 'merchant', 1, '2026-06-14 09:30:00'),
    (26, 'bbq03',     '$2b$10$eKGW8dORfl6Z1qRD3EvAbe038JSllAW6FnOzFnPmQEePCXqn/Z7Oy', '15800000009', 'merchant', 1, '2026-06-14 09:30:00');

-- ---------- 新增商家详情（9家）----------
INSERT INTO `merchant_details` (`id`, `user_id`, `shop_name`, `shop_address`, `shop_phone`, `description`, `logo_url`, `opening_time`, `closing_time`, `delivery_fee`, `min_order_amount`, `rating`, `created_at`, `enabled`)
VALUES
    -- 甜品：甜蜜时光
    (8,  18, '甜蜜时光',   '长沙市天心区坡子街80号',       '15800000001', '经典中式甜品·手工糖水·养生甜汤',   NULL, '11:00:00', '21:00:00', 3.00, 15.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 甜品：巴黎甜心
    (9,  19, '巴黎甜心',   '长沙市开福区中山路150号',      '15800000002', '法式甜品·精致蛋糕·手工慕斯',       NULL, '09:00:00', '20:00:00', 5.00, 20.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 甜品：冰凉一夏
    (10, 20, '冰凉一夏',   '长沙市雨花区万家丽路300号',    '15800000003', '手工冰淇淋·鲜果冰沙·浓醇奶昔',     NULL, '10:00:00', '22:00:00', 4.00, 15.00, 5.00, '2026-06-14 09:30:00', 1),

    -- 火锅：蜀九香火锅
    (11, 21, '蜀九香火锅',       '长沙市芙蓉区八一路88号',      '15800000004', '正宗川味火锅，麻辣鲜香，地道成都味',        NULL, '11:00:00', '23:00:00', 6.00, 50.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 火锅：潮牛记牛肉火锅
    (12, 22, '潮牛记牛肉火锅',   '长沙市岳麓区银盆南路60号',    '15800000005', '鲜切潮汕牛肉，原汤原味，每日现宰',          NULL, '11:00:00', '22:00:00', 5.00, 40.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 火锅：滇味菌汤锅
    (13, 23, '滇味菌汤锅',       '长沙市天心区芙蓉南路220号',   '15800000006', '云南野生菌汤，养生鲜香，不辣也够味',        NULL, '11:00:00', '21:30:00', 5.00, 45.00, 5.00, '2026-06-14 09:30:00', 1),

    -- 烧烤：火焰山烧烤
    (14, 24, '火焰山烧烤',       '长沙市开福区湘江中路180号',   '15800000007', '新疆风味烧烤，炭火现烤，大口吃肉',          NULL, '17:00:00', '02:00:00', 5.00, 30.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 烧烤：韩宫烤肉
    (15, 25, '韩宫烤肉',         '长沙市雨花区万家丽中路500号', '15800000008', '正宗韩式烤肉，生菜包肉，小菜无限续',        NULL, '11:00:00', '22:00:00', 4.00, 40.00, 5.00, '2026-06-14 09:30:00', 1),
    -- 烧烤：串江湖烤串
    (16, 26, '串江湖烤串',       '长沙市岳麓区大学城美食街10号', '15800000009', '江湖风味烤串，学生最爱，十年老摊',          NULL, '16:00:00', '01:00:00', 3.00, 20.00, 5.00, '2026-06-14 09:30:00', 1);

-- ---------- 新增产品分类（9家×3-4类，共29个分类）----------
INSERT INTO `product_categories` (`id`, `merchant_id`, `name`, `sort_order`, `created_at`)
VALUES
    -- 甜蜜时光 (merchant_id=8, 4类)
    (24, 8,  '经典糖水', 1, '2026-06-14 09:30:00'),
    (25, 8,  '糕点甜品', 2, '2026-06-14 09:30:00'),
    (26, 8,  '养生甜汤', 3, '2026-06-14 09:30:00'),
    (27, 8,  '冰品',     4, '2026-06-14 09:30:00'),

    -- 巴黎甜心 (merchant_id=9, 3类)
    (28, 9,  '蛋糕',     1, '2026-06-14 09:30:00'),
    (29, 9,  '慕斯布丁', 2, '2026-06-14 09:30:00'),
    (30, 9,  '面包小点', 3, '2026-06-14 09:30:00'),

    -- 冰凉一夏 (merchant_id=10, 3类)
    (31, 10, '冰淇淋',   1, '2026-06-14 09:30:00'),
    (32, 10, '圣代杯',   2, '2026-06-14 09:30:00'),
    (33, 10, '冰沙奶昔', 3, '2026-06-14 09:30:00'),

    -- 蜀九香火锅 (merchant_id=11, 4类)
    (34, 11, '锅底',     1, '2026-06-14 09:30:00'),
    (35, 11, '荤菜涮品', 2, '2026-06-14 09:30:00'),
    (36, 11, '素菜涮品', 3, '2026-06-14 09:30:00'),
    (37, 11, '主食小吃', 4, '2026-06-14 09:30:00'),

    -- 潮牛记牛肉火锅 (merchant_id=12, 3类)
    (38, 12, '汤底',     1, '2026-06-14 09:30:00'),
    (39, 12, '鲜切牛肉', 2, '2026-06-14 09:30:00'),
    (40, 12, '配菜涮品', 3, '2026-06-14 09:30:00'),

    -- 滇味菌汤锅 (merchant_id=13, 3类)
    (41, 13, '菌汤锅底', 1, '2026-06-14 09:30:00'),
    (42, 13, '菌菇涮品', 2, '2026-06-14 09:30:00'),
    (43, 13, '荤素搭配', 3, '2026-06-14 09:30:00'),

    -- 火焰山烧烤 (merchant_id=14, 3类)
    (44, 14, '羊肉系列', 1, '2026-06-14 09:30:00'),
    (45, 14, '牛肉系列', 2, '2026-06-14 09:30:00'),
    (46, 14, '素菜烤品', 3, '2026-06-14 09:30:00'),

    -- 韩宫烤肉 (merchant_id=15, 3类)
    (47, 15, '烤肉拼盘', 1, '2026-06-14 09:30:00'),
    (48, 15, '韩式小食', 2, '2026-06-14 09:30:00'),
    (49, 15, '饮品',     3, '2026-06-14 09:30:00'),

    -- 串江湖烤串 (merchant_id=16, 3类)
    (50, 16, '荤串', 1, '2026-06-14 09:30:00'),
    (51, 16, '素串', 2, '2026-06-14 09:30:00'),
    (52, 16, '主食', 3, '2026-06-14 09:30:00');

-- ---------- 新增产品（9家×12个=108个）----------
INSERT INTO `products` (`id`, `merchant_id`, `category_id`, `name`, `description`, `price`, `image_url`, `is_available`, `stock`, `created_at`)
VALUES
    -- ========================================
    -- 甜蜜时光 (merchant_id=8, products 69-80)
    -- ========================================
    -- 经典糖水 (cat 24)
    (69,  8, 24, '红豆双皮奶',   '顺德水牛奶，双层奶皮，绵密丝滑',             12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (70,  8, 24, '杨枝甘露',     '芒果西柚西米露，酸甜清凉',                   16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (71,  8, 24, '椰汁西米露',   '椰香浓郁，西米Q弹，经典港式糖水',             10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 糕点甜品 (cat 25)
    (72,  8, 25, '芒果班戟',     '薄皮包裹奶油芒果，一口满足',                 15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (73,  8, 25, '榴莲千层',     '层层榴莲果肉，入口即化',                     28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (74,  8, 25, '椰汁糕',       '冰凉Q弹，椰香清爽',                           8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 养生甜汤 (cat 26)
    (75,  8, 26, '银耳莲子羹',   '古田银耳配建宁莲子，清甜润肺',               12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (76,  8, 26, '红枣桂圆汤',   '新疆红枣，莆田桂圆，暖身养气',               10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (77,  8, 26, '百合雪梨汤',   '兰州百合配雪梨，润燥清心',                   11.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 冰品 (cat 27)
    (78,  8, 27, '芒果绵绵冰',   '台湾雪花冰，芒果酱淋面',                     18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (79,  8, 27, '红豆牛奶冰',   '绵密红豆配炼乳冰沙',                         14.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (80,  8, 27, '仙草芋圆冰',   '手工芋圆配仙草冻，清凉解暑',                 15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 巴黎甜心 (merchant_id=9, products 81-92)
    -- ========================================
    -- 蛋糕 (cat 28)
    (81,  9, 28, '黑森林蛋糕',   '樱桃酒香巧克力蛋糕，苦甜交织',               28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (82,  9, 28, '提拉米苏',     '意式经典，咖啡浸手指饼干配马斯卡彭',           26.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (83,  9, 28, '草莓奶油蛋糕', '新鲜草莓配动物奶油，轻盈不腻',                 22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (84,  9, 28, '巴斯克芝士',   '焦香表面，流心内里，芝士控必点',               24.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 慕斯布丁 (cat 29)
    (85,  9, 29, '芒果慕斯',     '新鲜芒果泥制成，细腻顺滑',                     18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (86,  9, 29, '巧克力熔岩',   '切开即流心，温热配冰淇淋绝佳',                 22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (87,  9, 29, '抹茶慕斯',     '日本宇治抹茶，微苦回甘',                       20.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (88,  9, 29, '焦糖布丁',     '法式经典，焦糖脆壳配嫩滑布丁',                 16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 面包小点 (cat 30)
    (89,  9, 30, '黄油可颂',     '法式千层酥皮，黄油香气扑鼻',                   12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (90,  9, 30, '肉桂卷',       '北欧经典，肉桂焦糖层层缠绕',                   14.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (91,  9, 30, '法式马卡龙4粒','外酥内软，四种口味随机',                       28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (92,  9, 30, '巧克力曲奇6块','酥脆掉渣，比利时黑巧',                         16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 冰凉一夏 (merchant_id=10, products 93-104)
    -- ========================================
    -- 冰淇淋 (cat 31)
    (93,  10, 31, '香草冰淇淋单球',   '马达加斯加香草荚，浓郁丝滑',                8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (94,  10, 31, '巧克力冰淇淋单球', '70%黑巧制成，微苦醇厚',                     8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (95,  10, 31, '草莓冰淇淋单球',   '鲜榨草莓果泥，酸甜清爽',                     8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (96,  10, 31, '抹茶冰淇淋单球',   '日本宇治抹茶，微苦回甘',                     8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (97,  10, 31, '三球缤纷拼盘',     '任选三款口味，一次满足',                    20.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 圣代杯 (cat 32)
    (98,  10, 32, '巧克力圣代',       '巧克力酱配脆皮碎，顶浇奶油',                15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (99,  10, 32, '草莓圣代',         '鲜草莓酱淋面，酸甜可口',                    14.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (100, 10, 32, '芒果圣代',         '大块芒果肉，热带风情',                      15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 冰沙奶昔 (cat 33)
    (101, 10, 33, '奥利奥暴风雪',     '奥利奥碎配香草冰淇淋，搅打绵密',            16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (102, 10, 33, '芒果冰沙',         '新鲜芒果打制，冰凉细腻',                    14.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (103, 10, 33, '草莓奶昔',         '鲜草莓与牛奶完美融合',                      16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (104, 10, 33, '香蕉巧克力奶昔',   '香蕉与巧克力的经典搭配',                    16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 蜀九香火锅 (merchant_id=11, products 105-116)
    -- ========================================
    -- 锅底 (cat 34)
    (105, 11, 34, '牛油麻辣锅底',       '正宗成都牛油锅底，麻辣醇厚',              38.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (106, 11, 34, '番茄鸳鸯锅底',       '一半麻辣一半番茄，各取所需',              42.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (107, 11, 34, '菌汤鸳鸯锅底',       '麻辣配菌汤，先喝汤再涮菜',                45.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 荤菜涮品 (cat 35)
    (108, 11, 35, '精品肥牛卷',         '雪花肥牛，入锅即熟',                      36.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (109, 11, 35, '鲜毛肚',             '新鲜毛肚，七上八下15秒',                  32.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (110, 11, 35, '手打虾滑',           '鲜虾手打，Q弹爽滑',                      28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (111, 11, 35, '鲜鸭肠',             '冰镇鲜鸭肠，脆爽弹牙',                    22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 素菜涮品 (cat 36)
    (112, 11, 36, '时蔬拼盘',           '土豆藕片娃娃菜菌菇组合',                  16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (113, 11, 36, '鲜豆腐',             '嫩滑豆腐，吸满汤汁',                       8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (114, 11, 36, '海带苗',             '鲜嫩海带苗，三秒即食',                    10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 主食小吃 (cat 37)
    (115, 11, 37, '红糖糍粑',           '外酥里糯，红糖浆黄豆粉',                  15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (116, 11, 37, '手工拉面',           '现拉面条，涮锅必备',                       8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 潮牛记牛肉火锅 (merchant_id=12, products 117-128)
    -- ========================================
    -- 汤底 (cat 38)
    (117, 12, 38, '牛骨清汤锅底',       '牛骨熬制6小时，汤清味浓',                 28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (118, 12, 38, '沙茶牛骨汤底',       '潮汕沙茶调味，浓香四溢',                   30.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 鲜切牛肉 (cat 39)
    (119, 12, 39, '吊龙伴',             '牛脊背上最嫩的部位，肥瘦相间',             48.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (120, 12, 39, '匙柄',               '牛肩胛肉，入口即化',                       42.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (121, 12, 39, '三花趾',             '牛前腿腱子肉，肉里包筋，嚼劲十足',           38.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (122, 12, 39, '胸口朥',             '牛胸口油，爽脆不腻',                       35.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (123, 12, 39, '嫩肉',               '后腿瘦肉，嫩滑无比',                       28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 配菜涮品 (cat 40)
    (124, 12, 40, '手打牛肉丸（8颗）',  '潮汕手打，Q弹爆汁',                       22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (125, 12, 40, '炸腐竹',             '油炸腐竹，吸汤神器',                       10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (126, 12, 40, '粿条',               '潮汕手工粿条，米香软滑',                   8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (127, 12, 40, '西洋菜',             '时令西洋菜，清甜去腻',                     8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (128, 12, 40, '玉米',               '甜玉米段，增鲜提味',                       6.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 滇味菌汤锅 (merchant_id=13, products 129-140)
    -- ========================================
    -- 菌汤锅底 (cat 41)
    (129, 13, 41, '松茸土鸡汤锅底',     '香格里拉松茸配土鸡，汤鲜至极',             58.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (130, 13, 41, '杂菌排骨汤锅底',     '六种菌菇与排骨慢炖',                       38.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 菌菇涮品 (cat 42)
    (131, 13, 42, '鲜松茸',             '香格里拉野生松茸，珍贵菌王',               68.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (132, 13, 42, '牛肝菌',             '云南野生牛肝菌，肉质肥厚',                 36.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (133, 13, 42, '竹荪',               '菌中皇后，口感脆嫩',                       28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (134, 13, 42, '鸡枞菌',             '鲜美无比，素食者的肉',                     42.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (135, 13, 42, '杂菌拼盘',           '金针菇杏鲍菇蟹味菇平菇组合',               22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 荤素搭配 (cat 43)
    (136, 13, 43, '乌鸡卷',             '滋补乌鸡，嫩滑养生',                       26.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (137, 13, 43, '云南火腿片',         '宣威火腿切片，咸鲜提味',                   32.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (138, 13, 43, '豌豆尖',             '云南高原豌豆尖，清甜鲜嫩',                 12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (139, 13, 43, '豆腐拼盘',           '冻豆腐、鲜豆腐、豆皮三拼',                 12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (140, 13, 43, '过桥米线',           '云南特色，涮煮美味',                       10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 火焰山烧烤 (merchant_id=14, products 141-152)
    -- ========================================
    -- 羊肉系列 (cat 44)
    (141, 14, 44, '红柳大串（5串）',    '红柳枝穿肉，果木炭烤，地道新疆味',          25.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (142, 14, 44, '烤羊排',             '精选羊肋排，孜然辣椒炭烤',                  38.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (143, 14, 44, '烤羊腰（2串）',      '新鲜羊腰，炭火慢烤',                        18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (144, 14, 44, '烤羊腿（小）',       '小羊腿慢烤，外焦里嫩',                      58.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 牛肉系列 (cat 45)
    (145, 14, 45, '烤牛肉串（10串）',   '牛里脊肉，嫩而不柴',                        22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (146, 14, 45, '烤牛板筋（5串）',    '嚼劲十足，越嚼越香',                        15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (147, 14, 45, '烤鸡翅（4只）',      '奥尔良风味烤翅，鲜嫩多汁',                  18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (148, 14, 45, '烤五花肉（5串）',    '肥瘦相间，焦香四溢',                        20.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 素菜烤品 (cat 46)
    (149, 14, 46, '烤韭菜',             '新鲜韭菜，蒜蓉调味',                         8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (150, 14, 46, '烤面筋（5串）',      'Q弹面筋，秘制酱料',                         10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (151, 14, 46, '烤土豆片',           '薄切土豆，外焦里糯',                         6.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (152, 14, 46, '烤金针菇',           '锡纸烤制，蒜香浓郁',                         8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 韩宫烤肉 (merchant_id=15, products 153-164)
    -- ========================================
    -- 烤肉拼盘 (cat 47)
    (153, 15, 47, '五花肉拼盘',         '厚切韩式五花肉，配生菜蒜片',               48.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (154, 15, 47, 'LA牛仔骨',           '韩式腌制牛排骨，甜咸入味',                 58.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (155, 15, 47, '调味牛五花',         '韩式酱料腌制牛五花，柔嫩多汁',             45.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (156, 15, 47, '猪颈肉',             '黄金六两肉，Q弹爽口',                       38.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (157, 15, 47, '韩式腌鸡肉',         '去骨鸡腿肉，辣酱腌制',                       28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 韩式小食 (cat 48)
    (158, 15, 48, '韩式泡菜饼',         '泡菜与面粉煎制，外脆内软',                   22.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (159, 15, 48, '石锅拌饭',           '蔬菜煎蛋配韩式辣酱，滋滋作响',               28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (160, 15, 48, '炒年糕',             '韩式辣酱炒年糕，软糯甜辣',                   18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (161, 15, 48, '炸鸡（半只）',       '韩式酱料炸鸡，外酥里嫩',                     28.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 饮品 (cat 49)
    (162, 15, 49, '韩式烧酒',           '真露烧酒360ml',                              18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (163, 15, 49, '大麦茶',             '冰镇大麦茶，解腻消食',                        8.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (164, 15, 49, '甜米露',             '韩式传统甜米露，清甜解辣',                   10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),

    -- ========================================
    -- 串江湖烤串 (merchant_id=16, products 165-176)
    -- ========================================
    -- 荤串 (cat 50)
    (165, 16, 50, '烤羊肉串（10串）',   '肥瘦相间羊肉，孜然辣椒',                     18.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (166, 16, 50, '烤牛肉串（10串）',   '牛里脊嫩串，焦香四溢',                       16.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (167, 16, 50, '烤鸡胗（5串）',      '脆嫩鸡胗，嚼劲十足',                         10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (168, 16, 50, '烤鱿鱼须（5串）',    '鲜嫩鱿鱼须，炭火快烤',                       15.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (169, 16, 50, '烤鸡皮（5串）',      '焦脆鸡皮，油脂丰润',                         10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 素串 (cat 51)
    (170, 16, 51, '烤面筋（5串）',      '秘制酱料刷烤，Q弹入味',                      8.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (171, 16, 51, '烤豆皮（5串）',      '薄豆皮卷，外焦里嫩',                          6.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (172, 16, 51, '烤茄子（整条）',     '蒜蓉粉丝烤茄子，软烂入味',                   12.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (173, 16, 51, '烤土豆片（5串）',    '薄切土豆，外焦里糯',                          6.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    -- 主食 (cat 52)
    (174, 16, 52, '烤馒头片（2个）',    '炭烤白馒头，外脆里软配炼乳',                  5.00,  NULL, 1, 9999, '2026-06-14 09:30:00'),
    (175, 16, 52, '蛋炒饭',             '粒粒分明，烧烤伴侣',                         10.00, NULL, 1, 9999, '2026-06-14 09:30:00'),
    (176, 16, 52, '烤冷面',             '东北特色烤冷面，酸甜酱汁',                   10.00, NULL, 1, 9999, '2026-06-14 09:30:00');











-- ================================================================
-- 已有商家各追加4个菜品（16家×4=64个）
-- ================================================================

INSERT INTO `products` (`id`, `merchant_id`, `category_id`, `name`, `description`, `price`, `image_url`, `is_available`, `stock`, `created_at`)
VALUES
    -- === 美味中餐馆 (merchant_id=1) ===
    (177, 1, 1, '酸辣鸡杂',   '鸡杂配酸豆角，开胃下饭',                26.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (178, 1, 1, '干锅花菜',   '花菜干煸，五花肉提香',                  22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (179, 1, 2, '手撕包菜',   '大火爆炒，酸辣脆爽',                    16.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (180, 1, 3, '炒粉',       '河粉猛火爆炒，锅气十足',                12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 西式快餐店 (merchant_id=2) ===
    (181, 2, 4, '奥尔良烤鸡堡','鲜嫩烤鸡腿肉，奥尔良风味',             20.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (182, 2, 5, '香辣鸡翅（4只）','外酥里嫩，香辣入味',                14.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (183, 2, 6, '鲜榨橙汁',   '新鲜橙子现榨，维C满满',                  10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (184, 2, 6, '柠檬水',     '鲜柠檬片泡制，冰爽解渴',                  6.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 家常私房菜 (merchant_id=3) ===
    (185, 3, 7, '糖醋排骨',   '酸甜酱汁包裹小排，软烂入味',             30.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (186, 3, 8, '凉拌海带丝', '酸辣调味，爽脆开胃',                      9.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (187, 3, 9, '冬瓜排骨汤', '清甜冬瓜配排骨，清淡滋补',               15.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (188, 3, 10,'葱花饼',     '现烙葱花饼，外酥里软',                    6.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 麦香汉堡 (merchant_id=4) ===
    (189, 4, 11,'照烧鸡腿堡', '日式照烧酱，甜咸多汁',                   22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (190, 4, 12,'上校鸡块（6块）','黄金酥脆，蘸酱更佳',                 12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (191, 4, 13,'冰美式咖啡', '现磨意式浓缩加冰，清爽提神',             12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (192, 4, 14,'儿童欢乐餐', '迷你汉堡+小薯+牛奶+玩具',                26.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 茶语时光 (merchant_id=5) ===
    (193, 5, 15,'黑糖珍珠奶茶','冲绳黑糖挂壁，珍珠Q弹',                 14.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (194, 5, 15,'抹茶拿铁',   '宇治抹茶配鲜牛奶，分层颜值',             18.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (195, 5, 16,'柠檬绿茶',   '茉莉绿茶底配新鲜柠檬片',                 10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (196, 5, 17,'卡布奇诺',   '意式浓缩配绵密奶泡',                     18.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 晨光早餐 (merchant_id=6) ===
    (197, 6, 18,'豆沙包（2个）','自制红豆沙，甜而不腻',                  4.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (198, 6, 18,'烧麦（4个）', '糯米烧麦，皮薄馅满',                     8.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (199, 6, 19,'绿豆粥',     '慢火熬煮绿豆，清热解暑',                  4.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (200, 6, 20,'煎蛋',       '溏心煎蛋，外焦里嫩',                      3.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 粥品世家 (merchant_id=7) ===
    (201, 7, 21,'青菜瘦肉粥', '碎肉青菜配白粥，清爽养胃',               10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (202, 7, 22,'酸辣萝卜条', '秘制泡菜萝卜，酸辣脆爽',                  5.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (203, 7, 23,'春卷（4个）','韭菜鸡蛋春卷，现炸酥脆',                 10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (204, 7, 23,'炸馒头片',   '裹蛋液油炸，蘸炼乳一绝',                  6.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 甜蜜时光 (merchant_id=8) ===
    (205, 8, 24,'黑芝麻糊',   '现磨黑芝麻，浓香顺滑',                   10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (206, 8, 25,'双皮奶',     '顺德水牛奶，厚实奶皮',                   14.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (207, 8, 26,'冰糖雪梨',   '雪梨炖冰糖枸杞，润燥清火',               12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (208, 8, 27,'西瓜冰',     '新鲜西瓜榨冰，清凉一夏',                 15.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 巴黎甜心 (merchant_id=9) ===
    (209, 9, 28,'红丝绒蛋糕', '经典红色丝绒，奶油芝士霜',               30.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (210, 9, 29,'酸奶慕斯',   '希腊酸奶制成，清爽酸甜',                  18.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (211, 9, 30,'葡式蛋挞（2个）','酥脆挞皮配嫩滑蛋液',                 16.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (212, 9, 30,'法棍切片',   '法式长棍切片，配黄油',                    12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 冰凉一夏 (merchant_id=10) ===
    (213, 10, 31,'薄荷巧克力冰淇淋','清凉薄荷配黑巧碎，双重口感',        10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (214, 10, 31,'椰子冰淇淋',     '泰国椰青制成，椰香浓郁',             10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (215, 10, 32,'蓝莓圣代',       '野生蓝莓酱淋面，酸甜可口',           16.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (216, 10, 33,'蜜桃冰沙',       '水蜜桃鲜果打制，夏日限定',           15.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 蜀九香火锅 (merchant_id=11) ===
    (217, 11, 35,'鲜黄喉',     '猪黄喉，涮30秒脆嫩弹牙',                 26.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (218, 11, 36,'鲜藕片',     '脆藕切片，清甜爽口',                      8.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (219, 11, 37,'红糖冰粉',   '手搓冰粉配红糖水，解辣神器',             10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (220, 11, 37,'现炸酥肉',   '花椒腌肉现炸，直接吃或涮皆可',           22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 潮牛记牛肉火锅 (merchant_id=12) ===
    (221, 12, 39,'牛舌',       '薄切牛舌，柔嫩多汁',                     38.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (222, 12, 40,'生菜',       '新鲜生菜，涮3秒即食',                     6.00,  NULL, 1, 9999, '2026-06-14 10:00:00'),
    (223, 12, 40,'白萝卜',     '切薄片，清甜化渣',                        5.00,  NULL, 1, 9999, '2026-06-14 10:00:00'),
    (224, 12, 40,'金针菇',     '新鲜金针菇，吸汤入味',                    8.00,  NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 滇味菌汤锅 (merchant_id=13) ===
    (225, 13, 42,'黑松露',     '云南黑松露切片，菌中之王',               88.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (226, 13, 42,'虫草花',     '金黄虫草花，爽脆滋补',                   22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (227, 13, 43,'手工虾滑',   '鲜虾手打，Q弹鲜甜',                      28.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (228, 13, 43,'冬瓜',       '清甜冬瓜，吸满菌汤',                      6.00,  NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 火焰山烧烤 (merchant_id=14) ===
    (229, 14, 44,'烤羊蹄',     '卤制后炭烤，软糯入味',                   22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (230, 14, 45,'烤牛蹄筋（5串）','筋道弹牙，越嚼越香',                 18.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (231, 14, 46,'烤玉米',     '整根甜玉米炭烤，刷黄油',                   8.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (232, 14, 46,'烤豆腐',     '锡纸嫩豆腐，蒜蓉酱汁',                    10.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 韩宫烤肉 (merchant_id=15) ===
    (233, 15, 47,'调味猪排',   '韩式酱料腌制猪排，厚实多汁',             42.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (234, 15, 48,'韩式冷面',   '冰镇荞麦面配酸辣汤底',                   22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (235, 15, 48,'部队锅（小份）','午餐肉拉面年糕泡菜一锅烩',            32.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (236, 15, 49,'韩国米酒',   '马格利米酒750ml，酸甜解腻',              22.00, NULL, 1, 9999, '2026-06-14 10:00:00'),

    -- === 串江湖烤串 (merchant_id=16) ===
    (237, 16, 50,'烤鸡爪（5串）','先卤后烤，软糯脱骨',                   12.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (238, 16, 51,'烤藕片（5串）','脆藕炭烤，香辣入味',                    6.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (239, 16, 51,'烤豆角（5串）','新鲜豆角，蒜香炭烤',                    6.00, NULL, 1, 9999, '2026-06-14 10:00:00'),
    (240, 16, 52,'烤年糕',     '韩式年糕炭烤，外脆里糯蘸炼乳',            8.00, NULL, 1, 9999, '2026-06-14 10:00:00');




-- 1. 扩展 users 表 status 注释（支持待审核）
ALTER TABLE users 
MODIFY COLUMN status TINYINT DEFAULT 1 NOT NULL COMMENT '0=禁用, 1=启用, 2=待审核';

-- 2. 商品分类表：添加上下架状态字段
ALTER TABLE product_categories 
ADD COLUMN is_available TINYINT DEFAULT 1 NOT NULL COMMENT '1=上架, 0=下架';

-- 3. 订单表：添加库存恢复标识字段（防止重复恢复库存）
ALTER TABLE orders 
ADD COLUMN stock_restored TINYINT DEFAULT 0 NOT NULL COMMENT '0=未恢复, 1=已恢复';

-- 4. 审计日志表（新建）
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(30) NOT NULL COMMENT '目标类型: USER/MERCHANT/RIDER/ORDER',
    target_id INT COMMENT '目标ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: UPDATE_STATUS/CREATE/DELETE',
    detail TEXT COMMENT '操作详情',
    target_name VARCHAR(100) COMMENT '目标名称',
    operator_id INT COMMENT '操作人ID(管理员)',
    operator_ip VARCHAR(45) COMMENT '操作人IP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target_type, target_id),
    INDEX idx_operator (operator_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作审计日志';

-- 文件：demo2/qualification_schema.sql，在 food 数据库中运行
CREATE TABLE IF NOT EXISTS `qualification_documents` (
                                                         `id` INT AUTO_INCREMENT PRIMARY KEY,
                                                         `user_id` INT NOT NULL COMMENT '申请人用户ID',
                                                         `user_role` VARCHAR(20) NOT NULL COMMENT '角色：merchant / rider',
                                                         `doc_type` VARCHAR(30) NOT NULL COMMENT '证件类型：business_license / id_card / health_cert',
                                                         `doc_url` VARCHAR(500) NOT NULL COMMENT '证件图片路径',
                                                         `review_status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '审核状态：pending/approved/rejected',
                                                         `review_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核备注/驳回原因',
                                                         `reviewed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
                                                         `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                         KEY `idx_user_role_status` (`user_id`, `user_role`, `review_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;