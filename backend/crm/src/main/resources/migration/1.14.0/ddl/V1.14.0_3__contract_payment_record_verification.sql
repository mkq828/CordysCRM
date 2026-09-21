-- 回款核销：为回款记录增加核销/撤回相关字段
-- 存量回款默认已完成核销（DONE），新回款默认待核销（PENDING）
ALTER TABLE contract_payment_record
  ADD COLUMN verification_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '核销状态 PENDING待核销/DONE已完成',
  ADD COLUMN verify_user VARCHAR(32) NULL COMMENT '核销人',
  ADD COLUMN verify_time BIGINT NULL COMMENT '核销时间',
  ADD COLUMN verify_remark VARCHAR(500) NULL COMMENT '核销备注',
  ADD COLUMN verify_proof VARCHAR(1000) NULL COMMENT '收款证明附件ID(逗号分隔)',
  ADD COLUMN revoke_user VARCHAR(32) NULL COMMENT '撤回人',
  ADD COLUMN revoke_time BIGINT NULL COMMENT '撤回时间',
  ADD COLUMN revoke_remark VARCHAR(500) NULL COMMENT '撤回备注';

UPDATE contract_payment_record SET verification_status = 'DONE';
