SET SESSION innodb_lock_wait_timeout = 7200;

-- 声音克隆鉴权改造（方案A）：豆包语音旧版应用只支持三件套鉴权（AppID + AccessToken + SecretKey），
-- 在 agent_model 上为「豆包语音」provider 补充三件套凭据字段，api_key 保留给方舟/阿里云等单串 key 场景。
ALTER TABLE agent_model
    ADD COLUMN app_id VARCHAR(100) NULL COMMENT '火山语音 AppID（豆包语音三件套鉴权）' AFTER api_key,
    ADD COLUMN access_token VARCHAR(1000) NULL COMMENT '火山语音 AccessToken（豆包语音三件套鉴权）' AFTER app_id,
    ADD COLUMN secret_key VARCHAR(1000) NULL COMMENT '火山语音 SecretKey（豆包语音三件套鉴权，HMAC256 签名用）' AFTER access_token;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
