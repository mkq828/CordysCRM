SET SESSION innodb_lock_wait_timeout = 7200;

-- 客户主表新增私信画像快照列（来源 ai_dm_profile，线索转客户时带入，形成闭环）
ALTER TABLE customer ADD COLUMN dm_profile_json TEXT COMMENT '私信画像JSON(性别/年龄/城市/品类/喜好/话术汇总/置信度 + 主页简介/抖音号/粉丝/关注/获赞)';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
