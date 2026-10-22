-- 演示租户「标杆客户 / 标杆线索」详情页补全种子（幂等，可重复执行）
-- 用途：让「广州联创软件信息技术有限公司」客户详情 14 个页签、对应线索详情「跟进记录/计划」全部有数据，用于帮助中心截图/演示。
-- 覆盖：跟进记录、跟进计划、负责人记录、客户关系、协作成员、私信画像、会话军师 AI 洞察、线索跟进。
-- 执行：mysql -h127.0.0.1 -uroot -p --default-character-set=utf8mb4 cordys-crm < installer/demo-customer-detail-seed.sql

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @ORG = '468047840653369344';
SET @C   = '9100000000060000001';  -- 广州联创软件信息技术有限公司
SET @U1  = '910000000003100001';
SET @U2  = '910000000003100002';
SET @U3  = '910000000003100003';
SET @C1  = '9100000000060000001_c1'; -- 陈志强
SET @C2  = '9100000000060000001_c2'; -- 林晓
SET @OPP = '910000000008000001';     -- 商机

-- 1) 跟进记录（电话/到访/微信）
INSERT IGNORE INTO follow_up_record (id, customer_id, opportunity_id, type, content, organization_id, follow_time, follow_method, owner, contact_id, create_time, update_time, create_user, update_user, comment_count) VALUES
('910000000007200001', @C, @OPP, 'CUSTOMER', '首次电话沟通：客户对商机漏斗和回款提醒最感兴趣，已约下周上门演示标准版，重点介绍 AI 会话军师。', @ORG, 1791500000000, '2', @U1, @C1, 1791500000000, 1791500000000, @U1, @U1, 0),
('910000000007200002', @C, @OPP, 'CUSTOMER', '上门演示后客户认可产品价值，预算 5 万以内，关注私有化部署可能性，需尽快出方案和报价。', @ORG, 1791600000000, '1', @U1, @C1, 1791600000000, 1791600000000, @U1, @U1, 0),
('910000000007200003', @C, @OPP, 'CUSTOMER', '已发送方案与报价，客户内部汇报中；竞品对比我们价格略高，需强调整体拥有成本与 AI 能力差异。', @ORG, 1792000000000, '3', @U1, @C2, 1792000000000, 1792000000000, @U1, @U1, 0);

-- 2) 跟进计划
INSERT IGNORE INTO follow_up_plan (id, customer_id, opportunity_id, type, content, organization_id, owner, contact_id, estimated_time, method, status, converted, create_time, update_time, create_user, update_user, comment_count) VALUES
('910000000007300001', @C, @OPP, 'CUSTOMER', '本周五与客户电话确认方案反馈，并约老板参与二次演示时间。', @ORG, @U1, @C1, 1792500000000, '2', 'UNDERWAY', 0, 1792000000000, 1792000000000, @U1, @U1, 0),
('910000000007300002', @C, @OPP, 'CUSTOMER', '跟进客户内部汇报结果，确认部署周期与付款方式，争取推进到合同阶段。', @ORG, @U1, @C2, 1792800000000, '1', 'PREPARED', 0, 1792000000000, 1792000000000, @U1, @U1, 0);

-- 3) 负责人记录（历史 + 当前）
INSERT IGNORE INTO customer_owner (id, customer_id, owner, collection_time, end_time, operator, reason_id) VALUES
('910000000007400001', @C, @U2, 1791500000000, 1791800000000, @U1, NULL),
('910000000007400002', @C, @U1, 1791800000000, NULL, NULL, NULL);

-- 4) 客户关系（1 集团 + 2 子公司）
INSERT IGNORE INTO customer_relation (id, source_customer_id, target_customer_id, create_time) VALUES
('910000000007500001', '9100000000060000002', @C, 1791500000000), -- 上级集团：杭州启明
('910000000007500002', @C, '9100000000060000003', 1791500000000), -- 子公司：上海鼎盛
('910000000007500003', @C, '9100000000060000004', 1791500000000); -- 子公司：北京远航

-- 5) 协作成员（1 协作 + 1 只读）
INSERT IGNORE INTO customer_collaboration (id, create_time, update_time, create_user, update_user, user_id, customer_id, collaboration_type) VALUES
('910000000007600001', 1791600000000, 1791600000000, @U1, @U1, @U2, @C, 'COLLABORATION'),
('910000000007600002', 1791600000000, 1791600000000, @U1, @U1, @U3, @C, 'READ_ONLY');

-- 6) 私信画像快照
UPDATE customer SET dm_profile_json = '{"nickname":"联创软件-陈经理","name":"陈志强","gender":"男","age":"35-40岁","city":"广州","accountCategory":"企业号","category":"企业软件服务","preference":"关注 SaaS CRM、销售管理、数字化转型","summary":"抖音私信咨询企业管理系统，关注商机漏斗与回款提醒，预算 5 万以内，决策人陈经理。","confidence":"高","phone":"13800138001","wechat":"czq_lc2026","bio":"专注企业数字化，提供 CRM 与销售管理解决方案","uniqueId":"lianchuang_soft","followerCount":"12800","followingCount":"320","likedCount":"45000","isGroup":false}' WHERE id = @C;

-- 6b) 最新跟进时间/跟进人（客户画像头部，避免显示「—」）
UPDATE customer SET follow_time = 1792000000000, follower = @U1 WHERE id = @C;

-- 7) 会话军师 AI 洞察（客户画像页「AI 洞察」）
INSERT IGNORE INTO ai_analysis_result (id, organization_id, biz_type, biz_id, feature_code, title, result_json, create_time, update_time, create_user, update_user) VALUES
('910000000007700001', @ORG, 'customer', @C, 'ai_advisor', '会话军师分析', '{"intentScore":"72","signals":["客户已深度了解标准版功能，重点关注商机漏斗和回款提醒","客户认可产品价值并主动询问私有化部署","客户表示预算 5 万以内、希望本月内定方案","客户愿安排老板一起参与二次演示"],"objections":["认为标准版价格略高于竞品","担心与现有财务系统对接","需评估私有化部署周期"],"emotion":"客户态度积极，需求明确，决策链已上探到老板，成交意愿较强。","competitorMentions":["客户提及对比过一家同类 CRM，基础版价格低约 15%"],"churnRisk":"低，客户需求匹配度高且有明确预算与时间计划，关键在价格沟通与部署周期确认。","suggestedScripts":["陈总，您重点看商机漏斗和回款提醒，这两块正是我们帮销售团队提效的核心。标准版看起来比竞品贵一点，但 AI 会话军师、回款提醒都内置在年费里，后续不用再按次付费，整体拥有成本反而更低。"]}', 1792000000000, 1792000000000, @U1, @U1);

-- 8) 线索跟进（线索详情「跟进记录/计划」页签，type=CLUE，对应标杆线索 9100000000050000001）
INSERT IGNORE INTO follow_up_record (id, clue_id, type, content, organization_id, follow_time, follow_method, owner, create_time, update_time, create_user, update_user, comment_count) VALUES
('910000000007200004', '9100000000050000001', 'CLUE', '首次电话沟通：客户通过官网咨询企业管理系统，重点想了解商机漏斗和回款提醒，预算 5 万以内，已约定下周上门演示标准版。', @ORG, 1791200000000, '2', @U1, 1791200000000, 1791200000000, @U1, @U1, 0),
('910000000007200005', '9100000000050000001', 'CLUE', '上门演示标准版，客户认可商机漏斗与回款提醒两个模块，并询问私有化部署可能性，需要尽快出方案和报价。', @ORG, 1791300000000, '1', @U1, 1791300000000, 1791300000000, @U1, @U1, 0),
('910000000007200006', '9100000000050000001', 'CLUE', '微信同步方案与报价，客户内部汇报中，反馈整体报价略高于竞品，需强调整体拥有成本与 AI 能力差异。', @ORG, 1791400000000, '3', @U1, 1791400000000, 1791400000000, @U1, @U1, 0);

INSERT IGNORE INTO follow_up_plan (id, clue_id, type, content, organization_id, owner, estimated_time, method, status, converted, create_time, update_time, create_user, update_user, comment_count) VALUES
('910000000007300003', '9100000000050000001', 'CLUE', '本周五电话确认方案反馈，并约客户老板一起参与二次演示。', @ORG, @U1, 1791800000000, '2', 'UNDERWAY', 0, 1791400000000, 1791400000000, @U1, @U1, 0),
('910000000007300004', '9100000000050000001', 'CLUE', '跟进客户内部汇报结果，确认部署周期与付款方式，争取推进到转化阶段。', @ORG, @U1, 1792000000000, '1', 'PREPARED', 0, 1791400000000, 1791400000000, @U1, @U1, 0);
