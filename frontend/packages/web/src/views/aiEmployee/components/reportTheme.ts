/**
 * AI 员工「客户全景分析」HTML 报告主题。
 * 提炼自用户提供的参考稿（渐变 hero + KPI 指标卡 + 分区表格 + 洞察条 + 行动项），
 * AI 只输出语义 HTML（class 约定见后端提示词骨架），样式统一由 AiReportBlock 注入页面（.ai-report 作用域），避免每份报告重复携带 CSS。
 */
const AI_REPORT_CSS = `.ai-report{
  --navy:#0f2744;
  --navy-2:#1b3a5c;
  --ink:#1c2b3a;
  --sub:#5a6b7c;
  --muted:#8899a8;
  --line:#e4ebf2;
  --bg:#f2f6fa;
  --card:#ffffff;
  --green:#10b981;
  --green-bg:#e8f8f1;
  --amber:#f59e0b;
  --amber-bg:#fef6e7;
  --red:#ef4444;
  --red-bg:#fdeeee;
  --blue:#3b82f6;
  --blue-bg:#eaf2fe;
  --purple:#8b5cf6;
  --purple-bg:#f3effe;
}.ai-report *{margin:0;padding:0;box-sizing:border-box;}.ai-report{
  font-family:"PingFang SC","Hiragino Sans GB","Microsoft YaHei",-apple-system,BlinkMacSystemFont,sans-serif;
  color:var(--ink);
  line-height:1.7;
}.ai-report{
  max-width:1080px;
  margin:0 auto;
  animation:ai-report-fadeUp .7s cubic-bezier(.2,.8,.2,1) both;
}@keyframes ai-report-fadeUp{
  from{opacity:0;transform:translateY(18px);}
  to{opacity:1;transform:none;}
}.ai-report .hero{
  position:relative;
  background:linear-gradient(135deg,#12304f 0%,#1b4670 48%,#1f6b5e 100%);
  border-radius:24px;
  padding:38px 42px 34px;
  color:#fff;
  overflow:hidden;
  box-shadow:0 30px 60px -30px rgba(15,39,68,.55);
}.ai-report .hero::before{
  content:"";
  position:absolute;
  top:-90px;right:-70px;
  width:320px;height:320px;
  border-radius:50%;
  background:radial-gradient(circle,rgba(16,185,129,.45),transparent 68%);
  pointer-events:none;
}.ai-report .hero::after{
  content:"";
  position:absolute;
  bottom:-120px;left:-60px;
  width:300px;height:300px;
  border-radius:50%;
  background:radial-gradient(circle,rgba(59,130,246,.35),transparent 68%);
  pointer-events:none;
}.ai-report .hero-tag{
  display:inline-flex;
  align-items:center;
  gap:7px;
  font-size:12px;
  letter-spacing:1.5px;
  color:#9fe8cd;
  background:rgba(16,185,129,.16);
  border:1px solid rgba(16,185,129,.35);
  padding:5px 13px;
  border-radius:99px;
  position:relative;z-index:1;
  margin-bottom:18px;
}.ai-report .hero-tag::before{
  content:"";
  width:6px;height:6px;border-radius:50%;
  background:#2ee6a8;
  box-shadow:0 0 10px #2ee6a8;
}.ai-report .hero h1{
  position:relative;z-index:1;
  font-size:27px;
  font-weight:600;
  letter-spacing:1px;
  line-height:1.4;
}.ai-report .hero .cid{
  position:relative;z-index:1;
  font-size:13px;
  color:#a9c4dc;
  margin-top:8px;
  letter-spacing:.6px;
  font-variant-numeric:tabular-nums;
}.ai-report .hero-meta{
  position:relative;z-index:1;
  display:flex;
  flex-wrap:wrap;
  gap:10px;
  margin-top:24px;
}.ai-report .hero-chip{
  font-size:12.5px;
  color:#d8e8f4;
  background:rgba(255,255,255,.09);
  border:1px solid rgba(255,255,255,.16);
  padding:7px 15px;
  border-radius:10px;
  letter-spacing:.4px;
  backdrop-filter:blur(4px);
}.ai-report .kpis{
  display:grid;
  grid-template-columns:repeat(4,1fr);
  gap:16px;
  margin-top:22px;
}.ai-report .kpi{
  background:var(--card);
  border:1px solid var(--line);
  border-radius:18px;
  padding:22px 20px 20px;
  position:relative;
  overflow:hidden;
  box-shadow:0 12px 28px -22px rgba(15,39,68,.35);
  transition:transform .28s cubic-bezier(.2,.8,.2,1), box-shadow .28s ease;
}.ai-report .kpi:hover{
  transform:translateY(-4px);
  box-shadow:0 20px 38px -22px rgba(15,39,68,.45);
}.ai-report .kpi-label{
  font-size:12.5px;
  color:var(--muted);
  letter-spacing:.6px;
  margin-bottom:10px;
}.ai-report .kpi-value{
  font-size:29px;
  font-weight:700;
  letter-spacing:-.5px;
  font-variant-numeric:tabular-nums;
  line-height:1.1;
  display:flex;
  align-items:baseline;
  gap:6px;
}.ai-report .kpi-value small{
  font-size:13px;
  font-weight:500;
  color:var(--muted);
  letter-spacing:0;
}.ai-report .kpi-foot{
  font-size:12px;
  color:var(--sub);
  margin-top:9px;
  letter-spacing:.3px;
}.ai-report .kpi-bar{
  height:5px;
  border-radius:99px;
  background:#eef3f8;
  margin-top:14px;
  overflow:hidden;
}.ai-report .kpi-bar span{
  display:block;
  height:100%;
  border-radius:99px;
  transition:width 1.2s cubic-bezier(.2,.8,.2,1);
}.ai-report .c-green{color:var(--green);}.ai-report .b-green{background:linear-gradient(90deg,#34d399,#10b981);}.ai-report .c-amber{color:var(--amber);}.ai-report .b-amber{background:linear-gradient(90deg,#fbbf24,#f59e0b);}.ai-report .c-red{color:var(--red);}.ai-report .b-red{background:linear-gradient(90deg,#f87171,#ef4444);}.ai-report .c-blue{color:var(--blue);}.ai-report .b-blue{background:linear-gradient(90deg,#60a5fa,#3b82f6);}.ai-report .c-purple{color:var(--purple);}.ai-report .b-purple{background:linear-gradient(90deg,#a78bfa,#8b5cf6);}.ai-report .section{
  background:var(--card);
  border:1px solid var(--line);
  border-radius:20px;
  padding:30px 32px 32px;
  margin-top:20px;
  box-shadow:0 14px 34px -28px rgba(15,39,68,.4);
}.ai-report .sec-head{
  display:flex;
  align-items:center;
  gap:13px;
  margin-bottom:22px;
  padding-bottom:18px;
  border-bottom:1px solid var(--line);
}.ai-report .sec-icon{
  width:38px;height:38px;
  flex:0 0 38px;
  border-radius:11px;
  display:grid;
  place-items:center;
  font-size:18px;
  line-height:1;
}.ai-report .i-blue{background:var(--blue-bg);}.ai-report .i-green{background:var(--green-bg);}.ai-report .i-amber{background:var(--amber-bg);}.ai-report .i-red{background:var(--red-bg);}.ai-report .i-purple{background:var(--purple-bg);}.ai-report .sec-head h2{
  font-size:17.5px;
  font-weight:600;
  letter-spacing:.8px;
  color:var(--ink);
}.ai-report .sec-head p{
  font-size:12.5px;
  color:var(--muted);
  letter-spacing:.4px;
  margin-top:2px;
}.ai-report .tbl{
  width:100%;
  border-collapse:separate;
  border-spacing:0;
  font-size:13.5px;
  overflow:hidden;
  border-radius:14px;
  border:1px solid var(--line);
}.ai-report .tbl thead th{
  background:#f7fafd;
  color:var(--sub);
  font-weight:600;
  text-align:left;
  font-size:12.5px;
  letter-spacing:.8px;
  padding:13px 18px;
  border-bottom:1px solid var(--line);
  white-space:nowrap;
}.ai-report .tbl tbody td{
  padding:15px 18px;
  border-bottom:1px solid #eef3f8;
  vertical-align:top;
  color:#33434f;
  line-height:1.72;
}.ai-report .tbl tbody tr:last-child td{border-bottom:none;}.ai-report .tbl tbody tr{transition:background .18s ease;}.ai-report .tbl tbody tr:hover{background:#fafcfe;}.ai-report .tbl .k{
  color:var(--sub);
  white-space:nowrap;
  font-weight:500;
  width:132px;
}.ai-report .tbl .num{
  font-variant-numeric:tabular-nums;
  font-weight:600;
  letter-spacing:.2px;
}.ai-report .badge{
  display:inline-flex;
  align-items:center;
  gap:5px;
  font-size:12px;
  font-weight:500;
  padding:3px 10px;
  border-radius:7px;
  letter-spacing:.3px;
  white-space:nowrap;
  line-height:1.6;
}.ai-report .bg-green{background:var(--green-bg);color:#0d8a62;border:1px solid #c8ebdd;}.ai-report .bg-amber{background:var(--amber-bg);color:#b4740a;border:1px solid #f6e2bd;}.ai-report .bg-red{background:var(--red-bg);color:#c62f2f;border:1px solid #f7cccc;}.ai-report .bg-blue{background:var(--blue-bg);color:#1f66d0;border:1px solid #cfe1fb;}.ai-report .bg-purple{background:var(--purple-bg);color:#6d3fd4;border:1px solid #ded3fb;}.ai-report .bg-gray{background:#f1f5f9;color:#5a6b7c;border:1px solid #e2e9f0;}.ai-report .insight{
  display:flex;
  gap:14px;
  align-items:flex-start;
  background:linear-gradient(120deg,#fff9ec,#fef6e7);
  border:1px solid #f8e5c0;
  border-left:4px solid var(--amber);
  border-radius:12px;
  padding:17px 20px;
  margin-top:20px;
  font-size:13.5px;
  line-height:1.75;
  color:#7a5510;
}.ai-report .insight strong{color:#5d3f07;}.ai-report .insight .em{font-size:16px;line-height:1.4;}.ai-report .diag{
  display:flex;
  flex-direction:column;
  gap:14px;
}.ai-report .diag-item{
  display:grid;
  grid-template-columns:1.15fr .95fr 1.35fr;
  gap:0;
  border:1px solid var(--line);
  border-radius:14px;
  overflow:hidden;
  transition:box-shadow .25s ease, transform .25s ease;
}.ai-report .diag-item:hover{
  transform:translateY(-2px);
  box-shadow:0 16px 34px -26px rgba(15,39,68,.5);
}.ai-report .diag-cell{
  padding:16px 18px;
  font-size:13px;
  line-height:1.72;
}.ai-report .diag-cell + .diag-cell{border-left:1px solid var(--line);}.ai-report .diag-cell .dc-label{
  display:block;
  font-size:11px;
  letter-spacing:1px;
  color:var(--muted);
  margin-bottom:7px;
  text-transform:uppercase;
}.ai-report .diag-signal{background:#fbfdff;}.ai-report .diag-gap{background:#fffdf8;}.ai-report .diag-advice{background:#f8fdfb;}.ai-report .diag-signal .dc-
.diag-gap .dc-
.diag-advice .dc-


.actions{
  display:grid;
  gap:14px;
}.ai-report .act{
  display:grid;
  grid-template-columns:64px minmax(0,1fr) 190px;
  gap:0;
  border:1px solid var(--line);
  border-radius:15px;
  overflow:hidden;
  background:#fff;
  transition:box-shadow .25s ease, transform .25s ease;
}.ai-report .act:hover{
  transform:translateY(-2px);
  box-shadow:0 18px 38px -28px rgba(15,39,68,.5);
}.ai-report .act-no{
  background:linear-gradient(160deg,#fff5f5,#fdeeee);
  color:var(--red);
  display:flex;
  flex-direction:column;
  align-items:center;
  justify-content:center;
  gap:4px;
  font-weight:700;
  font-size:20px;
  letter-spacing:0;
  border-right:1px solid var(--line);
  padding:14px 0;
}.ai-report .act-no small{
  font-size:10px;
  font-weight:500;
  color:#d78a8a;
  letter-spacing:1px;
}.ai-report .act-body{
  padding:16px 20px;
}.ai-report .act-body h4{
  font-size:14px;
  font-weight:600;
  color:var(--ink);
  letter-spacing:.4px;
  margin-bottom:9px;
  display:flex;
  align-items:center;
  gap:8px;
  flex-wrap:wrap;
}.ai-report .act-body ul{
  list-style:none;
  display:flex;
  flex-direction:column;
  gap:6px;
}.ai-report .act-body li{
  position:relative;
  padding-left:16px;
  font-size:13px;
  line-height:1.72;
  color:#4a5a68;
}.ai-report .act-body li::before{
  content:"";
  position:absolute;
  left:2px;top:9px;
  width:5px;height:5px;
  border-radius:50%;
  background:#f0a8a8;
}.ai-report .act-meta{
  background:#fbfdff;
  border-left:1px solid var(--line);
  padding:16px 18px;
  display:flex;
  flex-direction:column;
  gap:11px;
  justify-content:center;
}.ai-report .act-meta div{font-size:12px;}.ai-report .act-meta .ml{
  color:var(--muted);
  letter-spacing:.6px;
  display:block;
  margin-bottom:3px;
  font-size:11px;
}.ai-report .act-meta .mv{
  color:#33434f;
  font-weight:500;
  line-height:1.6;
}.ai-report .act-meta .mv.due{color:var(--red);font-weight:600;}.ai-report .long{
  display:grid;
  grid-template-columns:repeat(3,1fr);
  gap:16px;
}.ai-report .long-card{
  background:linear-gradient(180deg,#fbfdff,#f6fafd);
  border:1px solid var(--line);
  border-radius:15px;
  padding:22px 20px;
  transition:transform .28s cubic-bezier(.2,.8,.2,1), box-shadow .28s ease;
}.ai-report .long-card:hover{
  transform:translateY(-4px);
  box-shadow:0 20px 40px -28px rgba(15,39,68,.5);
}.ai-report .long-num{
  width:30px;height:30px;
  border-radius:9px;
  background:linear-gradient(145deg,#1b4670,#12304f);
  color:#fff;
  display:grid;
  place-items:center;
  font-size:13px;
  font-weight:700;
  margin-bottom:14px;
  box-shadow:0 8px 16px -10px rgba(15,39,68,.8);
}.ai-report .long-card h4{
  font-size:14px;
  font-weight:600;
  color:var(--ink);
  letter-spacing:.4px;
  margin-bottom:10px;
}.ai-report .long-card p{
  font-size:13px;
  line-height:1.78;
  color:#4a5a68;
}.ai-report .long-card p + p{margin-top:9px;}.ai-report .todo-card{
  background:linear-gradient(135deg,#12304f,#1b4670 55%,#1f6b5e);
  border-radius:20px;
  padding:30px 32px;
  color:#fff;
  margin-top:20px;
  box-shadow:0 30px 60px -34px rgba(15,39,68,.6);
}.ai-report .todo-card h3{
  font-size:16px;
  font-weight:600;
  letter-spacing:1px;
  margin-bottom:6px;
}.ai-report .todo-card .sub{
  font-size:12.5px;
  color:#a9c4dc;
  letter-spacing:.4px;
  margin-bottom:20px;
}.ai-report .todo-grid{
  display:grid;
  grid-template-columns:repeat(2,1fr);
  gap:10px 22px;
}.ai-report .todo-item{
  display:flex;
  align-items:flex-start;
  gap:10px;
  font-size:13px;
  color:#dceaf6;
  line-height:1.7;
  padding:9px 0;
  border-bottom:1px dashed rgba(255,255,255,.13);
}.ai-report .todo-item .tick{
  flex:0 0 18px;
  width:18px;height:18px;
  border-radius:5px;
  border:1.5px solid rgba(46,230,168,.6);
  display:grid;
  place-items:center;
  margin-top:2px;
  color:#2ee6a8;
  font-size:11px;
}.ai-report .quote{
  text-align:center;
  margin-top:26px;
  padding:26px 20px 4px;
}.ai-report .quote p{
  font-size:14.5px;
  color:var(--sub);
  letter-spacing:.6px;
  line-height:1.9;
  max-width:640px;
  margin:0 auto;
}.ai-report .quote strong{
  color:var(--ink);
  font-weight:600;
}@media (max-width:900px){
  .ai-report .kpis{grid-template-columns:repeat(2,1fr);}
  .ai-report .diag-item{grid-template-columns:1fr;}
  .ai-report .diag-cell + .diag-cell{border-left:none;border-top:1px solid var(--line);}
  .ai-report .act{grid-template-columns:56px minmax(0,1fr);}
  .ai-report .act-meta{
    grid-column:1 / -1;
    border-left:none;
    border-top:1px solid var(--line);
    flex-direction:row;
    flex-wrap:wrap;
    gap:18px;
  }
  .ai-report .long{grid-template-columns:1fr;}
  .ai-report .todo-grid{grid-template-columns:1fr;}
}@media (max-width:600px){
  
  .ai-report .hero{padding:28px 22px 26px;border-radius:18px;}
  .ai-report .hero h1{font-size:21px;}
  .ai-report .section{padding:22px 18px 24px;border-radius:16px;}
  .ai-report .kpis{grid-template-columns:1fr 1fr;gap:11px;}
  .ai-report .kpi{padding:17px 15px 15px;border-radius:14px;}
  .ai-report .kpi-value{font-size:23px;}
  .ai-report .tbl{font-size:12.5px;}
  .ai-report .tbl thead th,.tbl tbody td{padding:11px 12px;}
  .ai-report .tbl .k{width:auto;display:block;padding-bottom:4px;}
  .ai-report .todo-card{padding:24px 20px;}
  .ai-report .act{grid-template-columns:48px minmax(0,1fr);}
  .ai-report .act-no{font-size:17px;}
  .ai-report .act-body{padding:14px 15px;}
  .ai-report .act-meta{padding:14px 15px;}
}
`;

export default AI_REPORT_CSS;
