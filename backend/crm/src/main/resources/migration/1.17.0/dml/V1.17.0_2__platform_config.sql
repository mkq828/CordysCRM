-- 平台收款主体全局配置（上线前填入；回款收款账户/发票抬头默认值来源）
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('platform.companyName', '', 'text');
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('platform.bankName', '', 'text');
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('platform.bankAccount', '', 'text');
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('platform.invoiceTitle', '', 'text');
INSERT INTO sys_parameter (param_key, param_value, type)
VALUES ('platform.taxRate', '6', 'text');
