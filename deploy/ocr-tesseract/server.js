#!/usr/bin/env node
/**
 * 截图 OCR 服务（会话军师截图通道用）。
 *
 * 后端 SalesAdvisorService 按 PaddleOCR hubserving 的 HTTP 契约调用：
 *   POST /predict/ocr_system   body: {"images": ["<base64>", ...]}
 *   → {"results": [[{"text": "..."}, ...], ...]}   （每个元素是一页，页内是逐行 {text}）
 *
 * 本机装不了 PaddlePaddle（无 Python 3.14 / macOS 的 wheel），改用已安装的 Tesseract
 * 作为识别引擎，暴露与 PaddleOCR hubserving 完全一致的 HTTP 契约，因此后端无需改动。
 *
 * 依赖：Node（内置 http）、tesseract（brew install tesseract）+ chi_sim 语言包。
 * 启动：node server.js [端口，默认 8866]
 */
'use strict';

const http = require('http');
const { execFile } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');

const PORT = Number(process.argv[2] || 8866);
const HOST = '127.0.0.1';
const TESS_LANG = process.env.OCR_LANG || 'chi_sim+eng';
const TESS_PSM = process.env.OCR_PSM || '3';

function runTesseract(imagePath) {
  return new Promise((resolve, reject) => {
    execFile(
      'tesseract',
      [imagePath, 'stdout', '-l', TESS_LANG, '--psm', TESS_PSM],
      { maxBuffer: 10 * 1024 * 1024 },
      (err, stdout, stderr) => {
        if (err) {
          reject(new Error((stderr || err.message).trim() || 'tesseract failed'));
          return;
        }
        resolve(stdout);
      }
    );
  });
}

async function recognizeOne(base64) {
  // 兼容 data:image/png;base64, 前缀
  const raw = base64.replace(/^data:image\/[a-zA-Z]+;base64,/, '');
  const buf = Buffer.from(raw, 'base64');
  if (buf.length === 0) {
    throw new Error('empty image');
  }
  const tmp = path.join(os.tmpdir(), `ocr-${crypto.randomUUID()}.png`);
  try {
    fs.writeFileSync(tmp, buf);
    const text = await runTesseract(tmp);
    // 逐行拆成 {text}，去掉空行
    return text
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line.length > 0)
      .map((line) => ({ text: line }));
  } finally {
    fs.rmSync(tmp, { force: true });
  }
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

function send(res, code, obj) {
  const body = JSON.stringify(obj);
  res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/') {
    send(res, 200, { status: 'ok', engine: 'tesseract', lang: TESS_LANG });
    return;
  }
  if (req.method !== 'POST' || req.url !== '/predict/ocr_system') {
    send(res, 404, { error: 'not found' });
    return;
  }
  try {
    const body = JSON.parse(await readBody(req));
    const images = Array.isArray(body.images) ? body.images : [];
    const pages = [];
    for (const img of images) {
      pages.push(await recognizeOne(img));
    }
    send(res, 200, { results: pages });
  } catch (e) {
    send(res, 500, { error: String(e && e.message ? e.message : e) });
  }
});

server.listen(PORT, HOST, () => {
  console.log(`OCR service (tesseract ${TESS_LANG}, psm=${TESS_PSM}) on http://${HOST}:${PORT}/predict/ocr_system`);
});
