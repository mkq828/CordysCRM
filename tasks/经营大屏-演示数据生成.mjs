// 经营大屏演示数据生成器：为 admin 租户(100001)生成完整漏斗链路的演示数据
// 覆盖维度：客户等级/经营状态/行业/来源/省份地址、线索进度、商机阶段、订单阶段、
//          合同阶段+金额(近12月)、回款状态+金额(近12月)、跟进类型/方式、销售排行(多owner)
import fs from 'node:fs';

const ORG = '100001';
const ADMIN = 'admin';
const ADMIN_PW = '$2y$10$YJapMg8v0CTbaVq6WeF0SOoNco8Wi2e/Hz19gcDSt7GIDv6kjKlsi';

// 字段 id（org 100001 的 customer/clue 表单）
const F = {
  customerLevel: '462178097827946642', // 客户等级 1~5
  customerBusinessStatus: '482293388182396929', // 企业经营状态 1~6
  customerIndustry: '462178097827946641', // 客户行业
  customerSource: '462178097827946644', // 客户来源
  customerArea: '462178097827946648', // 地址(LOCATION, 存区县 adcode)
  clueProgress: '462178097827946576', // 线索进度 1/2
};

function rid(n) {
  return '91' + String(n).padStart(16, '0');
}

// 确定性随机
function mulberry32(a) {
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
const rand = mulberry32(20261013);

function ts(y, m, d, h = 10, min = 0) {
  return new Date(y, m - 1, d, h, min, 0).getTime();
}
function esc(s) {
  return String(s).replace(/'/g, "''");
}

// 近12月：2025-11 ~ 2026-10
const months = [];
for (let i = 0; i < 12; i++) {
  const d = new Date(2025, 10 + i, 1);
  months.push({ y: d.getFullYear(), m: d.getMonth() + 1 });
}
const monthOf = (i) => months[i % 12];

// 销售用户（6 个演示销售 + admin = 7 个 owner）
const sales = [
  { id: rid(1100001), name: '王伟', phone: '13900000001' },
  { id: rid(1100002), name: '李娜', phone: '13900000002' },
  { id: rid(1100003), name: '张强', phone: '13900000003' },
  { id: rid(1100004), name: '刘洋', phone: '13900000004' },
  { id: rid(1100005), name: '陈静', phone: '13900000005' },
  { id: rid(1100006), name: '赵磊', phone: '13900000006' },
];
const owners = [ADMIN, ...sales.map((s) => s.id)];

// 省份 + 区县 adcode（每省 2 个区县，10 省各 10 客户）
const provinces = [
  { city: ['深圳', '广州', '佛山'], adcodes: ['440305', '440103', '440604'] },
  { city: ['南京', '苏州'], adcodes: ['320102', '320505'] },
  { city: ['杭州', '宁波'], adcodes: ['330106', '330205'] },
  { city: ['上海'], adcodes: ['310115', '310104'] },
  { city: ['北京'], adcodes: ['110108', '110105'] },
  { city: ['成都'], adcodes: ['510107', '510108'] },
  { city: ['武汉'], adcodes: ['420106', '420111'] },
  { city: ['济南', '青岛'], adcodes: ['370102', '370214'] },
  { city: ['福州', '厦门'], adcodes: ['350103', '350203'] },
  { city: ['长沙'], adcodes: ['430103', '430111'] },
];
const industries = ['软件信息技术', '互联网', '智能制造', '零售电商', '金融服务', '教育培训', '医疗健康', '物流供应链', '建筑工程', '新能源'];
const sources = ['官网咨询', '行业展会', '客户转介绍', '广告投放', '抖音获客', '电话陌拜'];
const brands = ['云启', '智造', '华信', '博远', '恒达', '锐进', '联创', '中科', '天工', '盛世', '启明', '鼎盛', '众成', '浩宇', '嘉禾', '融信', '拓普', '新锐', '卓越', '宏图'];

const out = [];
out.push('-- ===== 经营大屏演示数据（幂等：先清 91% 前缀旧演示数据）=====');
out.push('DELETE FROM follow_up_record WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM contract_payment_record WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM contract WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM sales_order WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM opportunity WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM clue WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM customer WHERE organization_id = \'100001\' AND id LIKE \'91%\';');
out.push('DELETE FROM customer_field WHERE resource_id LIKE \'91%\';');
out.push('DELETE FROM clue_field WHERE resource_id LIKE \'91%\';');
out.push('DELETE FROM sys_organization_user WHERE user_id LIKE \'91%\';');
out.push('DELETE FROM sys_user WHERE id LIKE \'91%\';');
out.push('');

// ===== 销售用户 =====
const now = ts(2026, 10, 1);
out.push('-- 销售用户（sys_user + sys_organization_user）');
sales.forEach((s, idx) => {
  out.push(
    `INSERT INTO sys_user (id, name, phone, password, language, gender, create_time, update_time, create_user, update_user) VALUES ('${s.id}', '${esc(s.name)}', '${s.phone}', '${ADMIN_PW}', 'zh_CN', 0, ${now}, ${now}, 'admin', 'admin');`
  );
  out.push(
    `INSERT INTO sys_organization_user (id, organization_id, user_id, enable, create_time, update_time, create_user, update_user) VALUES ('${rid(1200001 + idx)}', '${ORG}', '${s.id}', 1, ${now}, ${now}, 'admin', 'admin');`
  );
});
out.push('');

// ===== 客户 100 条 =====
const customers = [];
const CUSTOMER_COUNT = 100;
out.push('-- 客户 100 条');
for (let i = 0; i < CUSTOMER_COUNT; i++) {
  const id = rid(1000001 + i);
  const prov = provinces[i % provinces.length];
  const city = prov.city[Math.floor(rand() * prov.city.length)];
  const industry = industries[i % industries.length];
  const name = `${city}${brands[Math.floor(rand() * brands.length)]}${industry}有限公司`;
  const owner = owners[i % owners.length];
  // 8 条落在本月(2026-10)，其余均匀分布在近12月
  const inMonth = i < 8;
  const createTime = inMonth
    ? ts(2026, 10, 3 + (i % 8))
    : (() => { const mo = monthOf(i); return ts(mo.y, mo.m, 3 + (i % 25), 9 + (i % 8)); })();
  const level = String((i % 5) + 1); // A~E 各 20
  const bizStatus = ['1', '1', '1', '1', '1', '2', '2', '3', '3', '5', '5', '6', '4'][i % 13]; // 在营偏多，各>=5
  const source = sources[i % sources.length];
  const area = prov.adcodes[Math.floor(rand() * prov.adcodes.length)];

  customers.push({ id, name, owner, createTime });
  out.push(
    `INSERT INTO customer (id, name, owner, create_time, update_time, create_user, update_user, organization_id) VALUES ('${id}', '${esc(name)}', '${owner}', ${createTime}, ${createTime}, 'admin', 'admin', '${ORG}');`
  );
  // 客户自定义字段
  out.push(`INSERT INTO customer_field (id, resource_id, field_id, field_value) VALUES ('${rid(7000001 + i * 5 + 0)}', '${id}', '${F.customerLevel}', '${level}');`);
  out.push(`INSERT INTO customer_field (id, resource_id, field_id, field_value) VALUES ('${rid(7000001 + i * 5 + 1)}', '${id}', '${F.customerBusinessStatus}', '${bizStatus}');`);
  out.push(`INSERT INTO customer_field (id, resource_id, field_id, field_value) VALUES ('${rid(7000001 + i * 5 + 2)}', '${id}', '${F.customerIndustry}', '${esc(industry)}');`);
  out.push(`INSERT INTO customer_field (id, resource_id, field_id, field_value) VALUES ('${rid(7000001 + i * 5 + 3)}', '${id}', '${F.customerSource}', '${esc(source)}');`);
  out.push(`INSERT INTO customer_field (id, resource_id, field_id, field_value) VALUES ('${rid(7000001 + i * 5 + 4)}', '${id}', '${F.customerArea}', '${area}');`);
}
out.push('');

// ===== 线索 140 条（未转化）=====
const CLUE_COUNT = 140;
out.push('-- 线索 140 条（未转化，漏斗第一层）');
const clues = [];
for (let i = 0; i < CLUE_COUNT; i++) {
  const id = rid(2000001 + i);
  const prov = provinces[i % provinces.length];
  const city = prov.city[Math.floor(rand() * prov.city.length)];
  const name = `${city}${brands[Math.floor(rand() * brands.length)]}${industries[i % industries.length]}有限公司`;
  const owner = owners[i % owners.length];
  const inMonth = i < 5;
  const createTime = inMonth
    ? ts(2026, 10, 2 + i)
    : (() => { const mo = monthOf(i); return ts(mo.y, mo.m, 2 + (i % 26), 8 + (i % 9)); })();
  const progress = i % 2 === 0 ? '1' : '2'; // 初步意向/强烈意愿 各 70
  clues.push({ id, owner, createTime });
  out.push(
    `INSERT INTO clue (id, name, owner, stage, organization_id, create_time, update_time, create_user, update_user) VALUES ('${id}', '${esc(name)}', '${owner}', 'NEW', '${ORG}', ${createTime}, ${createTime}, 'admin', 'admin');`
  );
  out.push(`INSERT INTO clue_field (id, resource_id, field_id, field_value) VALUES ('${rid(7100001 + i)}', '${id}', '${F.clueProgress}', '${progress}');`);
}
out.push('');

// ===== 商机 70 条（7 阶段各 10）=====
const OPP_COUNT = 70;
const oppStages = ['CREATE', 'CLEAR_REQUIREMENTS', 'SCHEME_VALIDATION', 'PROJECT_PROPOSAL_REPORT', 'BUSINESS_PROCUREMENT', 'SUCCESS', 'FAIL'];
out.push('-- 商机 70 条');
for (let i = 0; i < OPP_COUNT; i++) {
  const id = rid(2100001 + i);
  const customer = customers[i].id;
  const owner = owners[i % owners.length];
  const stage = oppStages[Math.floor(i / 10) % oppStages.length];
  const amount = Math.round((5 + rand() * 45) * 1000); // 5000~50000
  const mo = monthOf(i);
  const createTime = ts(mo.y, mo.m, 4 + (i % 24), 9 + (i % 8));
  const name = `${customers[i].name.slice(0, 4)}项目`;
  out.push(
    `INSERT INTO opportunity (id, customer_id, name, amount, organization_id, stage, owner, update_user, create_time, update_time, create_user) VALUES ('${id}', '${customer}', '${esc(name)}', ${amount}, '${ORG}', '${stage}', '${owner}', 'admin', ${createTime}, ${createTime}, 'admin');`
  );
}
out.push('');

// ===== 订单 45 条（7 阶段）=====
const ORDER_COUNT = 45;
const orderStages = ['CREATE', 'PENDING_SHIPMENT', 'PARTIALLY_SHIPPED', 'SHIPPED', 'PENDING_ACCEPTANCE', 'COMPLETED', 'VOIDED'];
out.push('-- 订单 45 条');
for (let i = 0; i < ORDER_COUNT; i++) {
  const id = rid(3000001 + i);
  const customer = customers[i].id;
  const owner = owners[i % owners.length];
  const stage = orderStages[i % orderStages.length];
  const amount = Math.round((5 + rand() * 45) * 1000);
  const mo = monthOf(i);
  const createTime = ts(mo.y, mo.m, 5 + (i % 23), 9 + (i % 8));
  const number = `SO${mo.y}${String(mo.m).padStart(2, '0')}${String(i + 1).padStart(3, '0')}`;
  out.push(
    `INSERT INTO sales_order (id, number, name, customer_id, owner, amount, stage, approval_status, organization_id, create_time, update_time, create_user, update_user, approved) VALUES ('${id}', '${number}', '${esc(customers[i].name.slice(0, 4))}订单', '${customer}', '${owner}', ${amount}, '${stage}', 'NONE', '${ORG}', ${createTime}, ${createTime}, 'admin', 'admin', 0);`
  );
}
out.push('');

// ===== 合同 32 条（6 非作废阶段各 5 + 作废 2）=====
const CONTRACT_COUNT = 30; // 非作废
const VOID_COUNT = 2; // 作废（void_reason 非空，不计入 KPI/阶段分布）
const contractStages = ['PENDING_SIGNING', 'SIGNED', 'CHANGE', 'IN_PROGRESS', 'COMPLETED_PERFORMANCE', 'ARCHIVED'];
const contracts = [];
out.push('-- 合同 32 条（30 非作废 + 2 作废）');
for (let i = 0; i < CONTRACT_COUNT; i++) {
  const id = rid(4000001 + i);
  const customer = customers[i].id;
  const owner = owners[i % owners.length];
  const stage = contractStages[Math.floor(i / 5) % contractStages.length];
  const amount = Math.round((5 + rand() * 295) * 10000); // 5万~300万
  const mo = monthOf(i);
  const startTime = ts(mo.y, mo.m, 3 + (i % 5) * 5, 9 + (i % 8));
  const endTime = startTime + 365 * 24 * 3600 * 1000;
  const number = `HT${mo.y}${String(mo.m).padStart(2, '0')}${String(i + 1).padStart(3, '0')}`;
  contracts.push({ id, owner, amount, startTime, customer });
  out.push(
    `INSERT INTO contract (id, name, customer_id, owner, amount, number, approval_status, stage, organization_id, create_time, update_time, create_user, update_user, start_time, end_time, approved) VALUES ('${id}', '${esc(customers[i].name.slice(0, 6))}合同', '${customer}', '${owner}', ${amount}, '${number}', 'NONE', '${stage}', '${ORG}', ${startTime}, ${startTime}, 'admin', 'admin', ${startTime}, ${endTime}, 0);`
  );
}
for (let i = 0; i < VOID_COUNT; i++) {
  const id = rid(4001000 + i);
  const customer = customers[CONTRACT_COUNT + i].id;
  const owner = owners[(CONTRACT_COUNT + i) % owners.length];
  const mo = monthOf(CONTRACT_COUNT + i);
  const startTime = ts(mo.y, mo.m, 6, 10);
  out.push(
    `INSERT INTO contract (id, name, customer_id, owner, amount, number, approval_status, stage, organization_id, void_reason, create_time, update_time, create_user, update_user, start_time, end_time, approved) VALUES ('${id}', '${esc(customers[CONTRACT_COUNT + i].name.slice(0, 6))}合同', '${customer}', '${owner}', 120000, 'HT${mo.y}${String(mo.m).padStart(2, '0')}9${i}1', 'NONE', 'VOID', '${ORG}', '测试作废', ${startTime}, ${startTime}, 'admin', 'admin', ${startTime}, ${startTime + 365 * 24 * 3600 * 1000}, 0);`
  );
}
out.push('');

// ===== 回款 45 条（30 DONE + 15 PENDING，近12月）=====
out.push('-- 回款 45 条（30 已核销 + 15 待核销）');
let paySeq = 0;
for (let i = 0; i < CONTRACT_COUNT; i++) {
  const c = contracts[i];
  const mo = monthOf(i);
  const endTime = ts(mo.y, mo.m, 6 + (i % 20), 9 + (i % 8));
  const amount = Math.round(c.amount * (0.4 + rand() * 0.4)); // 已核销 40%~80%
  paySeq++;
  out.push(
    `INSERT INTO contract_payment_record (id, name, owner, contract_id, record_amount, record_end_time, organization_id, create_time, update_time, create_user, update_user, verification_status) VALUES ('${rid(5000001 + i)}', '回款-${paySeq}', '${c.owner}', '${c.id}', ${amount}, ${endTime}, '${ORG}', ${endTime}, ${endTime}, 'admin', 'admin', 'DONE');`
  );
}
for (let i = 0; i < 15; i++) {
  const c = contracts[i % CONTRACT_COUNT];
  const mo = monthOf(i + 5);
  const endTime = ts(mo.y, mo.m, 8 + (i % 18), 10 + (i % 6));
  const amount = Math.round(c.amount * (0.15 + rand() * 0.25)); // 待核销 15%~40%
  paySeq++;
  out.push(
    `INSERT INTO contract_payment_record (id, name, owner, contract_id, record_amount, record_end_time, organization_id, create_time, update_time, create_user, update_user, verification_status) VALUES ('${rid(5001000 + i)}', '回款-${paySeq}', '${c.owner}', '${c.id}', ${amount}, ${endTime}, '${ORG}', ${endTime}, ${endTime}, 'admin', 'admin', 'PENDING');`
  );
}
out.push('');

// ===== 跟进记录 60 条（CUSTOMER 35 + CLUE 25，方式 到访/电话）=====
const FOLLOW_COUNT = 60;
const followContents = [
  '初次沟通，客户对产品感兴趣，约下次详细演示。',
  '上门拜访，介绍解决方案，客户表示需内部评估。',
  '电话回访，确认需求细节，报价已发送。',
  '微信沟通，客户反馈预算有限，考虑分期。',
  '演示产品核心功能，客户认可度较高。',
  '跟进合同签署进度，法务审核中。',
  '回访使用情况，客户反馈良好，有增购意向。',
  '客户提出定制需求，已转交技术评估。',
];
out.push('-- 跟进记录 60 条');
for (let i = 0; i < FOLLOW_COUNT; i++) {
  const id = rid(6000001 + i);
  const owner = owners[i % owners.length];
  const isCustomer = i < 35;
  const mo = monthOf(i);
  const followTime = ts(mo.y, mo.m, 2 + (i % 26), 9 + (i % 8));
  const method = i % 2 === 0 ? '1' : '2'; // 1 到访 / 2 电话
  const content = followContents[i % followContents.length];
  if (isCustomer) {
    const customerId = customers[i % CUSTOMER_COUNT].id;
    out.push(
      `INSERT INTO follow_up_record (id, customer_id, type, content, organization_id, follow_time, follow_method, owner, create_time, update_time, create_user, update_user, comment_count) VALUES ('${id}', '${customerId}', 'CUSTOMER', '${esc(content)}', '${ORG}', ${followTime}, '${method}', '${owner}', ${followTime}, ${followTime}, 'admin', 'admin', 0);`
    );
  } else {
    const clueId = clues[i - 35].id;
    out.push(
      `INSERT INTO follow_up_record (id, type, clue_id, content, organization_id, follow_time, follow_method, owner, create_time, update_time, create_user, update_user, comment_count) VALUES ('${id}', 'CLUE', '${clueId}', '${esc(content)}', '${ORG}', ${followTime}, '${method}', '${owner}', ${followTime}, ${followTime}, 'admin', 'admin', 0);`
    );
  }
}
out.push('');

const sql = out.join('\n');
fs.writeFileSync('/tmp/seed_business_screen.sql', sql, 'utf8');
console.log(`Generated ${out.length} lines -> /tmp/seed_business_screen.sql`);
console.log(`customers=${CUSTOMER_COUNT} clues=${CLUE_COUNT} opportunities=${OPP_COUNT} orders=${ORDER_COUNT} contracts=${CONTRACT_COUNT + VOID_COUNT} payments=45 follows=${FOLLOW_COUNT} sales=${sales.length}`);
