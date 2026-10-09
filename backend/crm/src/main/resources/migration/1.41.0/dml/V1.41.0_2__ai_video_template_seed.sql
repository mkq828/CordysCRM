SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 创作中心-视频模板库预置模板（INSERT IGNORE 幂等，template_key 唯一）。
-- config_json 各字段：background 片头/片尾/无图底色(0xRRGGBB)、titleFontSize/titleColor 片头标题样式、
-- subtitleFontSize/subtitleColor/subtitleOutline/subtitleShadow/subtitleCenter 字幕样式(ASS 颜色 &HAABBGGRR)、
-- closingText 片尾引导文案、transition 转场(none/fade 片头片尾淡入淡出)、bgmFile/bgmVolume 预留 BGM(Phase 2 资产库)、
-- scriptStyle 口播脚本语气、imageStyle 配图画面风格(通用「无文字/无人脸」约束由代码统一追加)。
INSERT IGNORE INTO ai_video_template
    (id, template_key, name, description, color, soft_color, config_json, sort, enabled, create_time, update_time, create_user, update_user)
VALUES
    ('vtpl0001', 'default-talk', '默认·口播', '经典深色底 + 白字底部字幕，亲切自然的口播质感', '#7c3aed', '#ede9fe',
     '{"background":"0x1E1B2E","titleFontSize":96,"titleColor":"&H00FFFFFF","subtitleFontSize":64,"subtitleColor":"&H00FFFFFF","subtitleOutline":"&H00000000","subtitleShadow":"&HA0000000","subtitleCenter":false,"closingText":"关注主页 · 了解更多","transition":"none","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"亲切自然的口语化口播","imageStyle":"，竖屏9:16构图，高清写实电影质感"}',
     1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('vtpl0002', 'tech-blue', '科技蓝', '深空蓝底 + 冷调科技感画面，专业简洁、数据感', '#2563eb', '#dbeafe',
     '{"background":"0x0B1220","titleFontSize":96,"titleColor":"&H00FFFFFF","subtitleFontSize":64,"subtitleColor":"&H00FFFFFF","subtitleOutline":"&H00000000","subtitleShadow":"&HA0000000","subtitleCenter":false,"closingText":"关注主页 · 了解更多","transition":"fade","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"专业简洁、有科技数据感","imageStyle":"，竖屏9:16构图，冷调科技感，蓝青色高光，未来感"}',
     2, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('vtpl0003', 'guochao-red', '国潮红', '深红底 + 国潮插画风画面，热血有张力的促销感', '#e11d48', '#ffe4e6',
     '{"background":"0x3B0A0A","titleFontSize":96,"titleColor":"&H00FFFFFF","subtitleFontSize":64,"subtitleColor":"&H00FFFFFF","subtitleOutline":"&H00000000","subtitleShadow":"&HA0000000","subtitleCenter":false,"closingText":"马上行动 · 别错过","transition":"fade","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"热血有张力、强促销感","imageStyle":"，竖屏9:16构图，国潮插画风，红金配色，浓郁中式元素"}',
     3, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('vtpl0004', 'fresh-green', '清新绿', '墨绿底 + 清新自然画面，亲切种草、温柔治愈', '#16a34a', '#dcfce7',
     '{"background":"0x0C1F17","titleFontSize":96,"titleColor":"&H00FFFFFF","subtitleFontSize":64,"subtitleColor":"&H00FFFFFF","subtitleOutline":"&H00000000","subtitleShadow":"&HA0000000","subtitleCenter":false,"closingText":"关注主页 · 了解更多","transition":"fade","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"亲切种草、温柔治愈","imageStyle":"，竖屏9:16构图，清新自然，绿意生活，柔和光线"}',
     4, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('vtpl0005', 'minimal-white', '极简白', '浅色底 + 深色字，极简高级、大量留白', '#0d9488', '#ccfbf1',
     '{"background":"0xF5F5F5","titleFontSize":96,"titleColor":"&H00111111","subtitleFontSize":64,"subtitleColor":"&H00111111","subtitleOutline":"&H00FFFFFF","subtitleShadow":"&H00000000","subtitleCenter":false,"closingText":"关注主页 · 了解更多","transition":"none","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"克制高级、留白感","imageStyle":"，竖屏9:16构图，极简高级，浅色背景，大量留白"}',
     5, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin'),
    ('vtpl0006', 'viral-hook', '爆款·3秒钩子', '高对比冲击力画面，3 秒强钩子 + 悬念反转的快节奏爆款公式', '#f97316', '#ffedd5',
     '{"background":"0x1A0B2E","titleFontSize":96,"titleColor":"&H00FFFFFF","subtitleFontSize":64,"subtitleColor":"&H00FFFFFF","subtitleOutline":"&H00000000","subtitleShadow":"&HA0000000","subtitleCenter":false,"closingText":"关注主页 · 每天更新","transition":"fade","bgmFile":null,"bgmVolume":0.0,"scriptStyle":"3秒强钩子、悬念反转、快节奏爆款","imageStyle":"，竖屏9:16构图，高对比冲击力，电商爆款质感"}',
     6, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000, 'admin', 'admin');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
