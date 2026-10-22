export default {
  // 注册审核
  'platformManualUsage.registerAudit.intro': '企业注册的审核入口：通过后自动开通租户，驳回则通知申请人。',
  'platformManualUsage.registerAudit.screenshotCaption': '注册审核列表页',
  'platformManualUsage.registerAudit.what':
    '企业客户提交注册申请后，会在这里排队等审核。你在这里核对资质，决定「通过」还是「驳回」。个人注册不用审，提交就自动开通，不会出现在这里。',
  'platformManualUsage.registerAudit.where': '左侧菜单「租户全生命周期」→「注册审核」',
  'platformManualUsage.registerAudit.step1': '打开页面，列表默认显示「待审核」的申请。',
  'platformManualUsage.registerAudit.step2': '用姓名、企业名称或手机号搜索要找的申请。',
  'platformManualUsage.registerAudit.step3': '点该条申请的「审核」按钮，核对填写的企业信息。',
  'platformManualUsage.registerAudit.step4':
    '确认没问题就点「通过」——系统自动给这家企业开通租户（建账号、开免费试用）。',
  'platformManualUsage.registerAudit.step5': '要是不通过就点「驳回」，填上原因，申请人会收到短信通知。',
  'platformManualUsage.registerAudit.example':
    '新客户「XX 科技」提交了企业注册，你在列表里找到它，核对资质后点「通过」，它就自动开通了免费试用，出现在「付费用户」列表里。',
  'platformManualUsage.registerAudit.tip': '「通过」会立即开通，别误点；驳回前先看清申请人的信息再填原因。',

  // 付费用户
  'platformManualUsage.paidUser.intro': '所有已开通租户的总表，看版本、到期时间、签约/跟进经理。',
  'platformManualUsage.paidUser.screenshotCaption': '付费用户列表页',
  'platformManualUsage.paidUser.what':
    '这里能看到每个租户当前是什么版本、什么时候到期、归哪个签约/跟进经理。你管理租户生命周期、看谁快到期，都从这里查。',
  'platformManualUsage.paidUser.where': '左侧菜单「租户全生命周期」→「付费用户」',
  'platformManualUsage.paidUser.step1': '按组织名称、手机号、签约经理或跟进经理搜索。',
  'platformManualUsage.paidUser.step2': '看列表里的「版本」「到期时间」「剩余天数」等列。',
  'platformManualUsage.paidUser.step3': '需要调整试用期时，点右上角「试用设置」。',
  'platformManualUsage.paidUser.step4': '点某条租户，可进入详情查看或修改它的套餐信息。',
  'platformManualUsage.paidUser.example': '想知道「侯月」这家租户还剩几天到期，搜名字后看「剩余天数」列就知道。',
  'platformManualUsage.paidUser.tip': '首笔回款核销后，这里的状态会自动从「免费试用」变成「企业版」。',

  // 续费申请
  'platformManualUsage.planApplication.intro': '租户续费/升级的申请列表，你在这里核对到账后核销。',
  'platformManualUsage.planApplication.screenshotCaption': '续费申请列表页',
  'platformManualUsage.planApplication.what':
    '租户快到期时提交续费或升级申请，会在这里排队。你核对付款到账后点「通过」，租户的有效期就顺延。',
  'platformManualUsage.planApplication.where': '左侧菜单「租户全生命周期」→「续费申请」',
  'platformManualUsage.planApplication.step1': '打开页面，看「待处理」的申请。',
  'platformManualUsage.planApplication.step2': '点开申请，看版本变更、金额、有效期和付款凭证。',
  'platformManualUsage.planApplication.step3': '核对钱已经到账后，点「通过」完成核销。',
  'platformManualUsage.planApplication.step4': '没到账或信息不对，点「驳回」并填原因。',
  'platformManualUsage.planApplication.example':
    '客户「李俊」的基础版到期申请续费，你核对付款凭证已到账，点「通过」，他的到期时间顺延 365 天。',
  'platformManualUsage.planApplication.tip': '只有续费核销才会顺延有效期；驳回不会动他的套餐。',

  // 合同
  'platformManualUsage.contract.intro': '管理每个租户的合同，从草稿到归档。',
  'platformManualUsage.contract.screenshotCaption': '合同列表页',
  'platformManualUsage.contract.what':
    '每个企业客户签的合同都记在这里，状态从「草稿 → 待签署 → 已完成 → 归档」流转。合同金额会作为回款和开票的依据。',
  'platformManualUsage.contract.where': '左侧菜单「租户全生命周期」→「合同」',
  'platformManualUsage.contract.step1': '点「新建合同」，填租户、套餐版本、合同金额和订阅时长。',
  'platformManualUsage.contract.step2': '上传合同扫描件（第一版线下签，拍照或扫描即可）。',
  'platformManualUsage.contract.step3': '保存后按实际进度改状态，签完标成「已完成」。',
  'platformManualUsage.contract.example':
    '企业客户签了年费 1680 元的合同，你新建合同、填金额、传扫描件、标「已完成」，后面回款就能关联这张合同。',
  'platformManualUsage.contract.tip': '回款金额一般就等于合同金额，别填错。',

  // 回款
  'platformManualUsage.paymentRecord.intro': '记录每笔回款并核销到账，核销后租户才正式生效或续期。',
  'platformManualUsage.paymentRecord.screenshotCaption': '回款列表页',
  'platformManualUsage.paymentRecord.what':
    '客户打款后，你在这里记一笔回款、传付款凭证，再点「核销」确认到账。首笔核销会自动把租户开成企业版。',
  'platformManualUsage.paymentRecord.where': '左侧菜单「租户全生命周期」→「回款」',
  'platformManualUsage.paymentRecord.step1': '点「回款」新增一笔回款，选关联合同、填金额和收款方式。',
  'platformManualUsage.paymentRecord.step2': '上传付款凭证，此时状态是「待核销」。',
  'platformManualUsage.paymentRecord.step3': '确认钱到账后点「核销」，状态变「已完成」。',
  'platformManualUsage.paymentRecord.example':
    '侯月首款 3980 元到账，你新增回款并核销，系统自动把她的套餐开成「企业版」。',
  'platformManualUsage.paymentRecord.tip': '撤回核销只会退掉记账，不会关掉已经开通的企业版。',

  // 发票
  'platformManualUsage.invoice.intro': '给已核销回款开票，记录开票类型和状态。',
  'platformManualUsage.invoice.screenshotCaption': '发票列表页',
  'platformManualUsage.invoice.what': '客户要发票时，你在这里登记开票信息，开好后把状态改掉。',
  'platformManualUsage.invoice.where': '左侧菜单「租户全生命周期」→「发票」',
  'platformManualUsage.invoice.step1': '点「新增发票」，关联合同。',
  'platformManualUsage.invoice.step2': '选开票类型（普票/专票）、填开票金额。',
  'platformManualUsage.invoice.step3': '开好后把状态改成「已开票」。',
  'platformManualUsage.invoice.example': '侯月付了 3980 元要开普票，你新增发票、填金额，开好后标记完成。',
  'platformManualUsage.invoice.tip': '开票金额一般等于合同金额。',

  // 营收看板
  'platformManualUsage.revenue.intro': '一眼看合同金额、实收、待收、开票金额。',
  'platformManualUsage.revenue.screenshotCaption': '营收看板页',
  'platformManualUsage.revenue.what': '这里汇总平台的整体营收：签了多少合同、实际收了多少、还差多少、开了多少票。',
  'platformManualUsage.revenue.where': '左侧菜单「租户全生命周期」→「营收看板」',
  'platformManualUsage.revenue.step1': '选统计维度「按周 / 按月 / 按年」。',
  'platformManualUsage.revenue.step2': '点「刷新」看最新数字。',
  'platformManualUsage.revenue.step3': '看四张卡片：合同金额、实收金额、待收金额、开票金额。',
  'platformManualUsage.revenue.example': '要报本月营收，切「按月」后看「实收金额」是多少。',

  // 合伙人账号
  'platformManualUsage.cityAccount.intro': '管理城市合伙人账号，看每个人签约/跟进多少客户。',
  'platformManualUsage.cityAccount.screenshotCaption': '合伙人账号页',
  'platformManualUsage.cityAccount.what':
    '城市合伙人（也叫城市经理）是帮你拓客的伙伴。这里建账号、把租户分配给他们、看他们各自负责多少客户。',
  'platformManualUsage.cityAccount.where': '左侧菜单「城市合伙人」→「合伙人账号」',
  'platformManualUsage.cityAccount.step1': '点「新增经理」建一个合伙人账号。',
  'platformManualUsage.cityAccount.step2': '点「分配归属」把租户分给某个经理。',
  'platformManualUsage.cityAccount.step3': '看列表里每个人的「签约客户数」「跟进客户数」。',
  'platformManualUsage.cityAccount.example': '新入职一个城市经理，你建好账号后，把几家租户的「跟进经理」分配给他。',
  'platformManualUsage.cityAccount.tip': '经理离职时要禁用账号，并把他的客户转移给其他人。',

  // 业绩看板
  'platformManualUsage.cityDashboard.intro': '按城市合伙人看签约客户数、合同金额、回款金额。',
  'platformManualUsage.cityDashboard.screenshotCaption': '业绩看板页',
  'platformManualUsage.cityDashboard.what': '按人来汇总业绩：每个人签了多少客户、合同金额多少、回了多少款。',
  'platformManualUsage.cityDashboard.where': '左侧菜单「城市合伙人」→「业绩看板」',
  'platformManualUsage.cityDashboard.step1': '选统计维度（按周/按月/按年）。',
  'platformManualUsage.cityDashboard.step2': '点「刷新」。',
  'platformManualUsage.cityDashboard.step3': '看每个经理的「签约客户数」「合同金额」「回款金额」。',
  'platformManualUsage.cityDashboard.example': '想看「邓鹏」这个月签了多少，看他的「签约客户数」和「合同金额」两列。',

  // 版本套餐
  'platformManualUsage.edition.intro': '配置可卖的版本（基础版/企业版等）和价格、配额。',
  'platformManualUsage.edition.screenshotCaption': '版本套餐页',
  'platformManualUsage.edition.what':
    '这里定义你能卖哪些版本、每个版本多少钱、能坐多少人、送多少 AI 额度。租户开通时按这里的配置走。',
  'platformManualUsage.edition.where': '左侧菜单「平台运营」→「版本套餐」',
  'platformManualUsage.edition.step1': '点「新增版本」，填版本编码和名称。',
  'platformManualUsage.edition.step2': '填年价、首年价、人数上限、有效期天数、AI 月度配额。',
  'platformManualUsage.edition.step3': '保存后把状态改成「启用」。',
  'platformManualUsage.edition.example': '想加一个「旗舰版」，新增版本填价格和配额，启用后租户就能选了。',
  'platformManualUsage.edition.tip': '改价格或配额只影响之后新开通的租户，已开通的快照不变。',

  // 系统日志
  'platformManualUsage.log.intro': '查谁在什么时候做了什么，含登录日志和 AI 执行日志。',
  'platformManualUsage.log.screenshotCaption': '系统日志页',
  'platformManualUsage.log.what':
    '所有关键操作都会记在这里，包括谁操作的、改了什么、在哪个页面。出了事来这查是谁干的。',
  'platformManualUsage.log.where': '左侧菜单「平台运营」→「系统日志」',
  'platformManualUsage.log.step1': '切「操作日志 / 登录日志 / AI 执行日志」。',
  'platformManualUsage.log.step2': '按操作人、时间、操作类型、操作对象筛选。',
  'platformManualUsage.log.step3': '点开看明细。',
  'platformManualUsage.log.example': '有人误改了客户资料，你在「操作日志」里按操作人筛选，定位到是谁、什么时候改的。',
  'platformManualUsage.log.tip':
    '服务端报错不在这里，去隔壁「系统错误记录」按 traceId 查；定时任务跑没跑、为什么失败去「定时任务执行记录」查。',

  // 问题反馈
  'platformManualUsage.bugReport.intro': '用户提交的问题反馈，带页面和 traceId，方便定位。',
  'platformManualUsage.bugReport.screenshotCaption': '问题反馈页',
  'platformManualUsage.bugReport.what':
    '用户在使用中遇到问题，可以从「反馈」入口提交，带页面路由和 traceId。你在这里集中处理和跟进。',
  'platformManualUsage.bugReport.where': '左侧菜单「平台运营」→「问题反馈」',
  'platformManualUsage.bugReport.step1': '看列表，按描述或提交人搜索。',
  'platformManualUsage.bugReport.step2': '点开看问题描述、页面路由和 traceId。',
  'platformManualUsage.bugReport.step3': '按 traceId 去「系统日志」里查对应异常，处理后改状态。',
  'platformManualUsage.bugReport.example':
    '用户反馈某个页面报错，你按 traceId 在日志里查到异常，修完后把这条反馈标记已处理。',

  // AI 额度管理
  'platformManualUsage.aiQuota.label': '额度包与计费规则',
  'platformManualUsage.aiQuota.intro': '平台侧的 AI 计费与额度：看成本、配单价、发额度包。',
  'platformManualUsage.aiQuota.screenshotCaption': 'AI 额度管理页',
  'platformManualUsage.aiQuota.what':
    '这是平台统一管理 AI 花钱和发额度的地方：看花了多少、每个模型怎么计价、给租户发多少额度。',
  'platformManualUsage.aiQuota.where': '左侧菜单「AI 额度管理」',
  'platformManualUsage.aiQuota.step1': '「成本看板」看总成本、Token 和调用次数。',
  'platformManualUsage.aiQuota.step2': '「模型单价」配每个模型的价格，这就是计费规则。',
  'platformManualUsage.aiQuota.step3': '「全局配置」设默认额度等全局参数。',
  'platformManualUsage.aiQuota.step4': '「额度包」创建额度包，「租户额度」给具体租户分配，「额度包订单」看购买记录。',
  'platformManualUsage.aiQuota.example':
    '新签的企业版租户要送 1000 次 AI 额度，你在「额度包」里建包、在「租户额度」里分配给它。',
  'platformManualUsage.aiQuota.tip': '平台 admin 账号自己用 AI 不扣额度。',

  // 安全事件
  'platformManualUsage.securityEvent.intro': '记录系统检测到的恶意请求与攻击行为，可导出取证。',
  'platformManualUsage.securityEvent.screenshotCaption': '安全事件列表页',
  'platformManualUsage.securityEvent.what':
    '系统会实时检测 SQL 注入、跨站脚本(XSS)、路径穿越、扫描器探测、高频报错这几类恶意请求，只记录、不拦截。你在这里查看谁在什么时候攻击了系统，必要时导出 CSV 作为安全取证。',
  'platformManualUsage.securityEvent.where': '左侧菜单「平台运营」→「安全事件」',
  'platformManualUsage.securityEvent.step1': '打开页面，按事件类型、IP、时间筛选。',
  'platformManualUsage.securityEvent.step2':
    '点某条的「查看」，看 IP、User-Agent、请求 URI、转发链、归属地、traceId 等详情。',
  'platformManualUsage.securityEvent.step3': '要留证就点「导出 CSV」，把当前筛选结果导出来。',
  'platformManualUsage.securityEvent.example':
    '某 IP 反复扫你的登录接口，你在「安全事件」里按该 IP 筛选，看到一串「扫描器」事件，导出 CSV 存证。',
  'platformManualUsage.securityEvent.tip': '安全事件只记录、不自动拦截；如果发现真有攻击，去防火墙或 WAF 上封 IP。',

  // 系统错误记录
  'platformManualUsage.errorLog.intro': '集中看服务端报错，按 traceId 定位，带异常堆栈。',
  'platformManualUsage.errorLog.screenshotCaption': '系统错误记录页',
  'platformManualUsage.errorLog.what':
    '服务端每次报错都会记一条，包含报错类、错误信息、异常堆栈、traceId 和发生时间。用户反馈问题、或页面报 500 时，来这按 traceId 精确定位。',
  'platformManualUsage.errorLog.where': '左侧菜单「平台运营」→「系统错误记录」',
  'platformManualUsage.errorLog.step1': '按报错类、traceId 或错误信息关键词筛选。',
  'platformManualUsage.errorLog.step2': '点「查看堆栈」看完整异常栈，定位是哪段代码、哪条 SQL 出错。',
  'platformManualUsage.errorLog.step3': '用这条记录的 traceId 去「问题反馈」或用户反馈里对号，修复后回填。',
  'platformManualUsage.errorLog.example':
    '用户反馈某页面报 500，你从反馈里拿到 traceId，来这搜到对应报错，看堆栈定位到是空指针，修完确认不再报。',
  'platformManualUsage.errorLog.tip': '报错记录会越积越多，定期清理；排查时优先用 traceId，最快定位。',

  // 定时任务执行记录
  'platformManualUsage.jobExecLog.intro': '看每个定时任务跑没跑、耗时多久、失败原因。',
  'platformManualUsage.jobExecLog.screenshotCaption': '定时任务执行记录页',
  'platformManualUsage.jobExecLog.what':
    '系统里所有定时任务（比如 AI 员工定时跟进、晨报推送、数据汇总）每次执行都会记一条：任务名、状态（运行中/成功/失败）、开始结束时间、耗时。失败还会记失败信息。',
  'platformManualUsage.jobExecLog.where': '左侧菜单「平台运营」→「定时任务执行记录」',
  'platformManualUsage.jobExecLog.step1': '按任务名、状态、时间范围筛选。',
  'platformManualUsage.jobExecLog.step2': '看列表里的状态和耗时，找出跑失败或跑得特别慢的任务。',
  'platformManualUsage.jobExecLog.step3': '点开失败的记录看失败信息，定位原因后修复。',
  'platformManualUsage.jobExecLog.example':
    'AI 员工今天没自动跟进，你来这按任务名筛「AI 员工跟进」，发现状态是「失败」，点开看失败信息是接口超时。',
  'platformManualUsage.jobExecLog.tip': '定时任务已错峰到凌晨执行，凌晨失败白天记得来这看一眼。',
};
