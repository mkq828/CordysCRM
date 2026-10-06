SET SESSION innodb_lock_wait_timeout = 7200;

-- 私信获客画像（功能 8）：租户绑定的平台账号 + 登录态 Cookie 密文
CREATE TABLE ai_dm_account
(
    id                 VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id    VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    platform           VARCHAR(32)  NOT NULL COMMENT '平台: douyin/kuaishou/xiaohongshu',
    account_name       VARCHAR(128) COMMENT '账号昵称(绑定后回填)',
    unique_id          VARCHAR(64)  COMMENT '账号ID(抖音号)',
    cookie_ciphertext  TEXT         COMMENT '登录态Cookie密文(EncryptUtils.aesEncrypt)',
    status             VARCHAR(32)  NOT NULL DEFAULT 'LOGGED_IN' COMMENT '绑定状态: LOGGED_IN/CRAWLING/EXPIRED/FAILED',
    last_crawl_time    BIGINT       COMMENT '最近采集时间(毫秒)',
    last_error         VARCHAR(512) COMMENT '最近失败原因',
    create_time        BIGINT       COMMENT '创建时间(毫秒)',
    update_time        BIGINT       COMMENT '更新时间(毫秒)',
    create_user        VARCHAR(32)  COMMENT '创建人',
    update_user        VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_platform (organization_id, platform)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 私信联系人画像结果：原始消息 + 正则提取 + LLM 结构化画像 + 导入线索池关联
CREATE TABLE ai_dm_profile
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    account_id      VARCHAR(32)  COMMENT '来源账号(ai_dm_account.id)',
    platform        VARCHAR(32)  COMMENT '平台',
    sec_uid         VARCHAR(256) COMMENT '平台用户 sec_uid(抖音为 MS4wLjAB 开头的 base64，长度可达 140+)',
    nickname        VARCHAR(128) COMMENT '昵称',
    unique_id       VARCHAR(64)  COMMENT '账号ID(抖音号)',
    follower_count  VARCHAR(32)  COMMENT '粉丝数(群聊固定"群聊")',
    following_count VARCHAR(32)  COMMENT '关注数(主页刮取)',
    liked_count     VARCHAR(32)  COMMENT '获赞数(主页刮取)',
    gender          VARCHAR(8)   COMMENT '性别(主页「抖音号」后的男/女文本)',
    bio             VARCHAR(200) COMMENT '主页简介(签名/认证文本，用于 AI 提炼账号品类)',
    is_group        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否群聊',
    phone           VARCHAR(32)  COMMENT '正则提取手机号',
    wechat          VARCHAR(64)  COMMENT '正则提取微信号',
    messages_json   TEXT         COMMENT '私信原文(结构化 sender/content)',
    profile_json    TEXT         COMMENT '画像JSON(性别/年龄/城市/品类/喜好/话术汇总/置信度)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'RAW' COMMENT '状态: RAW/ANALYZED/IMPORTED/FAILED',
    clue_id         VARCHAR(32)  COMMENT '导入线索池后的线索ID',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_account (organization_id, account_id, create_time),
    KEY idx_org_sec (organization_id, sec_uid)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
