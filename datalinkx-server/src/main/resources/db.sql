CREATE database if NOT EXISTS `datalinkx` default character set utf8mb4 collate utf8mb4_unicode_ci;
use `datalinkx`;

SET NAMES utf8mb4;

CREATE TABLE `DS` (
    `id` int unsigned NOT NULL AUTO_INCREMENT,
    `ds_id` char(35) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `host` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `port` int DEFAULT NULL,
    `username` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `password` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `database` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
    `schema` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '数据库的原始schema',
    `config` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '附加配置',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `ds_id` (`ds_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='数据库信息';


CREATE TABLE `JOB` (
   `id` int unsigned NOT NULL AUTO_INCREMENT,
   `job_id` char(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
   `reader_ds_id` char(40) NOT NULL DEFAULT '' COMMENT '来源数据源id',
   `writer_ds_id` char(40) NOT NULL DEFAULT '' COMMENT '目标数据源id',
   `config` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
   `crontab` varchar(256) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
   `from_tb` char(40) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '来源数据表id',
   `to_tb` char(40) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '目标数据表id',
   `count` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '导出数据统计',
   `xxl_id` char(40) NOT NULL DEFAULT '' COMMENT 'xxl_job_id',
   `task_id` char(40) NOT NULL DEFAULT '' COMMENT 'flink_job_id',
   `sync_mode` longtext COMMENT 'mode: overwrite(全量)/increment(增量)，increate_field(增量字段)，increate_value(增量值)',
   `is_del` int NOT NULL DEFAULT '0',
   `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   `status` int NOT NULL DEFAULT '0' COMMENT '0 - 新建、1 - 运行中、2 - 运行成功、3 - 运行失败、4 - 停止、5 - 队列中',
   `error_msg` longtext CHARACTER SET utf8 COLLATE utf8_general_ci,
   PRIMARY KEY (`id`) USING BTREE,
   KEY `job_id` (`job_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流转任务配置信息';

CREATE TABLE `JOB_RELATION` (
    `id` int unsigned NOT NULL AUTO_INCREMENT,
    `relation_id` char(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '依赖关系id',
    `job_id` char(40) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '流转任务id',
    `sub_job_id` char(40) NOT NULL DEFAULT '' COMMENT '流转任务子id',
    `priority` int COMMENT '同级任务优先级，越高优先级越高',
    `is_del` int NOT NULL DEFAULT '0',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    KEY `job_id` (`job_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='流转任务级联配置表';


CREATE TABLE `JOB_LOG` (
    `id` int unsigned NOT NULL AUTO_INCREMENT,
    `cost_time` int unsigned NOT NULL,
    `count` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '读写条数',
    `start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务启动时间',
    `end_time` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务结束时间',
    `error_msg` longtext CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '异常信息',
    `job_id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `status` tinyint unsigned DEFAULT NULL,
    `is_del` int DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='流转任务日志';


alter table JOB ADD COLUMN  `name` varchar(64) DEFAULT NULL COMMENT '任务名称';;

alter table JOB ADD COLUMN `cover` tinyint NOT NULL DEFAULT '0' COMMENT '是否开启覆盖';

-- 流式任务表结构变更

alter table JOB ADD COLUMN `type` tinyint NOT NULL DEFAULT '0' COMMENT '是否是流式任务';

alter table JOB ADD COLUMN `checkpoint` longtext CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '流式任务端点续传';

alter table JOB ADD COLUMN `start_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP;

alter table JOB ADD COLUMN `retry_time` int NOT NULL DEFAULT '0' COMMENT '流式任务重试次数';

alter table JOB ADD COLUMN `graph` longtext CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '计算画布';

-- 旧数据暂不处理，需手动将type改为driver name
ALTER TABLE DS MODIFY COLUMN `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL;

-- 告警表

CREATE TABLE `ALARM_COMPONENT` (
    `id` int unsigned NOT NULL AUTO_INCREMENT,
    `alarm_id` char(35) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '',
    `name` char(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '组件名称',
    `type` int DEFAULT NULL COMMENT '0 email|1 钉钉',
    `config` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '附加配置',
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `alarm_id` (`alarm_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='告警组件配置';

CREATE TABLE `ALARM_RULE` (
    `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
    `rule_id` char(35) CHARACTER SET utf8 DEFAULT NULL COMMENT '规则id',
    `alarm_id` char(35) CHARACTER SET utf8 DEFAULT NULL COMMENT '告警组件id',
    `job_id` char(35) CHARACTER SET utf8 DEFAULT NULL COMMENT '流转任务id',
    `name` varchar(255) DEFAULT NULL,
    `status` tinyint(4) DEFAULT '1',
    `type` int(11) DEFAULT NULL COMMENT '0 结束后推送| 1 失败后推送| 2 成功后推送',
    `push_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `ctime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `utime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_del` int(11) DEFAULT '0',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `rule_id` (`rule_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='告警规则信息表';


-- 如果使用mysqlcdc，需要手动创建用户 用户名：canal 密码：Canal@123456
create user 'canal'@'%' identified by 'Canal@123456';
-- 授权 *.*表示所有库
grant SELECT, REPLICATION SLAVE, REPLICATION CLIENT on *.* to 'canal'@'%' identified by 'Canal@123456';
GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' IDENTIFIED BY 'Canal@123456' WITH GRANT OPTION