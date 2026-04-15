CREATE DATABASE IF NOT EXISTS `formly`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `formly`;

DROP TABLE IF EXISTS `form_submission`;
DROP TABLE IF EXISTS `form_version`;
DROP TABLE IF EXISTS `form_definition`;
DROP TABLE IF EXISTS `form_group`;

CREATE TABLE `form_group` (
  `id` BIGINT NOT NULL COMMENT '分组ID',
  `parent_id` BIGINT NULL COMMENT '父级分组ID，根分组为空',
  `name` VARCHAR(100) NOT NULL COMMENT '分组名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '同级排序值，越小越靠前',
  `created_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人标识',
  `updated_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '逻辑删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_group_parent_sort` (`parent_id`, `sort`),
  KEY `idx_form_group_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单分组';

CREATE TABLE `form_definition` (
  `id` BIGINT NOT NULL COMMENT '表单ID',
  `group_id` BIGINT NULL COMMENT '所属分组ID，允许为空表示根级表单',
  `name` VARCHAR(100) NOT NULL COMMENT '表单名称',
  `form_key` VARCHAR(100) NOT NULL COMMENT '表单唯一业务标识',
  `description` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '表单描述',
  `draft_schema_json` JSON NULL COMMENT '当前草稿Schema',
  `published_version_id` BIGINT NULL COMMENT '当前已发布版本ID',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '同级排序值，越小越靠前',
  `created_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人标识',
  `updated_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新人标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '逻辑删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_definition_form_key` (`form_key`),
  KEY `idx_form_definition_group_sort` (`group_id`, `sort`),
  KEY `idx_form_definition_published_version` (`published_version_id`),
  KEY `idx_form_definition_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单定义';

CREATE TABLE `form_version` (
  `id` BIGINT NOT NULL COMMENT '版本ID',
  `form_id` BIGINT NOT NULL COMMENT '所属表单ID',
  `version_no` INT NOT NULL COMMENT '递增版本号',
  `schema_json` JSON NOT NULL COMMENT '整份编辑器Schema',
  `published_at` DATETIME NULL DEFAULT NULL COMMENT '发布时间',
  `created_by` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建人标识',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '逻辑删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_version_form_id_version_no` (`form_id`, `version_no`),
  KEY `idx_form_version_form_created_at` (`form_id`, `created_at`),
  KEY `idx_form_version_published_at` (`published_at`),
  KEY `idx_form_version_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单历史版本';

CREATE TABLE `form_submission` (
  `id` BIGINT NOT NULL COMMENT '提交记录ID',
  `form_id` BIGINT NOT NULL COMMENT '表单ID',
  `form_version_id` BIGINT NOT NULL COMMENT '提交时使用的发布版本ID',
  `submitter` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '提交人标识',
  `submit_status` VARCHAR(32) NOT NULL DEFAULT 'submitted' COMMENT '提交状态',
  `data_json` JSON NOT NULL COMMENT '整份表单填写数据',
  `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '逻辑删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_form_submission_form_submitted_at` (`form_id`, `submitted_at`),
  KEY `idx_form_submission_version` (`form_version_id`),
  KEY `idx_form_submission_status` (`submit_status`),
  KEY `idx_form_submission_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='表单提交记录';
