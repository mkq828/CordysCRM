SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 额度包商品种子（价格/次数为占位，正式定价上线前可在 admin 端调整）
INSERT IGNORE INTO ai_quota_pack (id, feature_code, name, calls, price_fen, status, sort, remark, create_time, update_time, create_user, update_user)
VALUES
    ('pack_video_100', 'ai_video', '短视频生成包', 100, 5000, 1, 1, 'AI 短视频/漫剧生成专用次数', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('pack_ppt_50', 'ai_ppt', 'AI 方案/PPT 包', 50, 3000, 1, 2, 'AI 方案与 PPT 生成专用次数', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('pack_advisor_100', 'ai_advisor', '销售军师包', 100, 4000, 1, 3, '销售会话军师分析专用次数', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('pack_general_200', '', '全功能通用包', 200, 9900, 1, 9, '覆盖全部 AI 功能的通用次数', UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
