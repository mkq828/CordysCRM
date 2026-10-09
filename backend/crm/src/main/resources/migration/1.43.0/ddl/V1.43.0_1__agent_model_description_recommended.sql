SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频风格页「模型」区块需要展示模型描述与是否推荐；模型设置新增/编辑补充「描述」「推荐模型」两个字段
ALTER TABLE agent_model
    ADD COLUMN description VARCHAR(500) NULL COMMENT '模型描述（短视频风格页展示）' AFTER model_params,
    ADD COLUMN recommended TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐模型（1=推荐）' AFTER description;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
