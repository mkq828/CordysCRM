SET SESSION innodb_lock_wait_timeout = 7200;

-- 封存（待完善）功能标记为「规划中」：短视频 / 爆款复刻 / 漫剧 / 数字人(剪辑/复刻)。
-- 这些功能仍保留功能点与版本归属，仅在对比中显示「规划中/敬请期待」，不放给租户。
UPDATE sys_feature
SET launched = 0
WHERE feature_code IN ('ai_video', 'ai_replica', 'ai_drama', 'digital_human');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
