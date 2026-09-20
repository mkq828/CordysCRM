-- suggestion 系列表在 MySQL 9.x 默认 utf8mb4_0900_ai_ci 下创建，与 sys_user/sys_organization 的
-- utf8mb4_general_ci 不一致，导致跨表 JOIN 报 Illegal mix of collations，此处统一转换。
ALTER TABLE suggestion CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE suggestion_vote CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE suggestion_comment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
