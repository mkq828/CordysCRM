# 截图 OCR 服务（会话军师截图通道）

「AI 销售会话军师」的截图通道需要一个本地 OCR HTTP 服务。后端 `SalesAdvisorService` 按
PaddleOCR hubserving 的契约调用它：

```
POST /predict/ocr_system
body: {"images": ["<base64>", ...]}
→ {"results": [[{"text": "..."}, ...], ...]}   // results[i] = 第 i 张图，页内逐行 {text}
```

## 为什么用 Tesseract 而不是 PaddleOCR

计划里原定 PaddleOCR，但本机（macOS x86_64 + Python 3.14）**装不了 PaddlePaddle**（无对应
wheel，`pip index versions paddlepaddle` 直接 No matching distribution）。本机已装
`tesseract`（brew），故用 Tesseract 做识别引擎，`server.js` 只负责把它的输出包成与
PaddleOCR hubserving 完全一致的 HTTP 契约，**后端零改动**。

## 依赖（一次性）

```bash
brew install tesseract        # 本机已装
# 中文语言包（chi_sim），本机已放到 /usr/local/share/tessdata/
curl -sL -o /usr/local/share/tessdata/chi_sim.traineddata \
  https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata
tesseract --list-langs       # 应能看到 chi_sim
```

## 启动

```bash
node deploy/ocr-tesseract/server.js 8866
# 可选环境变量：OCR_LANG（默认 chi_sim+eng）、OCR_PSM（默认 3）
```

健康检查：`curl http://127.0.0.1:8866/` → `{"status":"ok","engine":"tesseract",...}`

## 配置（后端读 sys_parameter）

```sql
INSERT INTO `cordys-crm`.sys_parameter (param_key, param_value, type)
VALUES ('ocr.serviceUrl', 'http://127.0.0.1:8866', 'text')
ON DUPLICATE KEY UPDATE param_value = VALUES(param_value);
```

未配置 `ocr.serviceUrl` 时，截图通道会报「截图识别服务未部署，请联系管理员」，粘贴文本通道不受影响。
