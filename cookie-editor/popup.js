// 私信获客 Cookie 助手 —— popup 逻辑
// 用 chrome.cookies API 读抖音所有 Cookie（含 httpOnly 的 sessionid_ss），
// 拼成后端能识别的 header 串：name1=v1; name2=v2; ...

const REQUIRED = ['sessionid_ss', 'uid_tt'];

function getAllCookies() {
  return new Promise((resolve) => {
    chrome.cookies.getAll({ domain: 'douyin.com' }, (cookies) => resolve(cookies || []));
  });
}

function getActiveTab() {
  return new Promise((resolve) => {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => resolve(tabs && tabs[0] ? tabs[0] : null));
  });
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch (e) {
    // 旧内核回退方案
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    let ok = false;
    try {
      ok = document.execCommand('copy');
    } catch (_) {
      ok = false;
    }
    document.body.removeChild(ta);
    return ok;
  }
}

async function init() {
  const statusEl = document.getElementById('status');
  const reqEl = document.getElementById('required');
  const copyBtn = document.getElementById('copy');
  const hintEl = document.getElementById('hint');

  const tab = await getActiveTab();
  let onDouyin = false;
  try {
    onDouyin = /(^|\.)douyin\.com$/.test(new URL(tab.url).hostname);
  } catch (_) {
    onDouyin = false;
  }

  const cookies = await getAllCookies();
  const have = new Set(cookies.map((c) => c.name));
  const missing = REQUIRED.filter((k) => !have.has(k));

  // 顶部状态
  if (!onDouyin && cookies.length === 0) {
    statusEl.className = 'status err';
    statusEl.textContent = '请先打开并登录抖音网页版';
    hintEl.textContent = '在浏览器登录 https://www.douyin.com 后再点此扩展。';
  } else if (missing.length === 0) {
    statusEl.className = 'status ok';
    statusEl.textContent = '已登录抖音，登录态完整，可直接复制。';
  } else {
    statusEl.className = 'status warn';
    statusEl.textContent = '登录态不完整，缺少：' + missing.join('、');
    hintEl.textContent = '请先在 https://www.douyin.com 重新登录，再回来复制。';
  }

  // 关键项清单
  reqEl.innerHTML = REQUIRED.map((k) => {
    const ok = have.has(k);
    return '<div class="item ' + (ok ? 'ok' : 'miss') + '">' + (ok ? '✓' : '✗') + ' ' + k + '</div>';
  }).join('');

  copyBtn.disabled = cookies.length === 0;
  copyBtn.addEventListener('click', async () => {
    const text = cookies.map((c) => c.name + '=' + c.value).join('; ');
    const ok = await copyText(text);
    copyBtn.textContent = ok ? '已复制 ✓（共 ' + cookies.length + ' 项）' : '复制失败，请手动复制';
    setTimeout(() => {
      copyBtn.textContent = '复制 Cookie';
    }, 1600);
  });
}

document.addEventListener('DOMContentLoaded', init);
