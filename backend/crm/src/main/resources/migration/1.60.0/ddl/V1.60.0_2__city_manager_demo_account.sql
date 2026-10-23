-- 城市合伙人绑定演示账号：避免多人共用同一演示账号互相顶下线
ALTER TABLE platform_city_manager
    ADD COLUMN demo_user_id VARCHAR(32) NULL COMMENT '绑定的演示账号用户ID(sys_user.id)，离职时解绑' AFTER phone;
