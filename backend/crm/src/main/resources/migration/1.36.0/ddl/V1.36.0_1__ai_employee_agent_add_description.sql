SET SESSION innodb_lock_wait_timeout = 7200;

-- 岗位 Agent 新增岗位描述列（可选，添加时未填写则回退角色目录中该角色的描述）
ALTER TABLE ai_employee_agent ADD COLUMN description TEXT COMMENT '岗位描述(可选，为空时回退角色描述)';

SET SESSION innodb_lock_wait_timeout = DEFAULT;
