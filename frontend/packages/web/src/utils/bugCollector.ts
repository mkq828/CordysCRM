/**
 * 前端错误与失败请求采集器（单例模块）。
 * 供「反馈问题」悬浮按钮一键导出 .md 使用，收集最近运行错误与失败接口。
 */
import CDR from '@/api/http';

interface CollectedError {
  time: number;
  type: string;
  message: string;
  source?: string;
  lineno?: number;
  colno?: number;
  stack?: string;
}

interface FailedRequest {
  time: number;
  method: string;
  url: string;
  status?: number;
  requestBody?: string;
  responseBody?: string;
  traceId?: string;
}

const MAX_ERRORS = 20;
const MAX_FAILED_REQUESTS = 10;

const errors: CollectedError[] = [];
const failedRequests: FailedRequest[] = [];

let installed = false;

function push<T>(list: T[], item: T, max: number): void {
  list.push(item);
  if (list.length > max) {
    list.splice(0, list.length - max);
  }
}

function safeStringify(value: unknown): string {
  if (value === undefined || value === null) {
    return '';
  }
  try {
    return typeof value === 'string' ? value : JSON.stringify(value);
  } catch {
    return String(value);
  }
}

// 敏感字段脱敏，避免把密码 / token 写进导出的 .md
const SENSITIVE_KEYS = ['password', 'passwd', 'pwd', 'token', 'secret', 'csrf', 'session'];

function sanitize(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sanitize);
  }
  if (value && typeof value === 'object') {
    const result: Record<string, unknown> = {};
    Object.keys(value as Record<string, unknown>).forEach((key) => {
      const lower = key.toLowerCase();
      result[key] = SENSITIVE_KEYS.some((k) => lower.includes(k))
        ? '***'
        : sanitize((value as Record<string, unknown>)[key]);
    });
    return result;
  }
  return value;
}

function recordError(type: string, message: string, extra: Partial<CollectedError> = {}): void {
  push(errors, { time: Date.now(), type, message, ...extra }, MAX_ERRORS);
}

/**
 * 安装全局错误采集：运行时错误、Promise 拒绝、console.error、失败接口。
 * 仅需调用一次（App.vue onMounted）。
 */
export function installBugCollector(): void {
  if (installed) {
    return;
  }
  installed = true;

  // 运行时错误（用 addEventListener 与 App.vue 的 window.onerror 赋值共存）
  window.addEventListener('error', (event) => {
    recordError('error', event.message || '未知运行时错误', {
      source: event.filename,
      lineno: event.lineno,
      colno: event.colno,
      stack: event.error?.stack,
    });
  });

  // Promise 未处理拒绝
  window.addEventListener('unhandledrejection', (event) => {
    const { reason } = event;
    recordError('unhandledrejection', reason?.message ?? safeStringify(reason), {
      stack: reason?.stack,
    });
  });

  // console.error 拦截（保留原始行为）
  // eslint-disable-next-line no-console
  const originalConsoleError = console.error.bind(console);
  // eslint-disable-next-line no-console
  console.error = (...args: unknown[]) => {
    recordError('console.error', args.map(safeStringify).join(' '));
    originalConsoleError(...args);
  };

  // 失败接口（追加一个响应错误拦截器，不改动 createAxios 原有逻辑）
  CDR.axiosInstance.interceptors.response.use(
    (response) => response,
    (error: any) => {
      const config = error?.config ?? {};
      const response = error?.response ?? {};
      const headers = response?.headers ?? {};
      const traceId = headers['x-trace-id'] ?? headers['X-Trace-Id'];
      push(
        failedRequests,
        {
          time: Date.now(),
          method: String(config?.method ?? 'GET').toUpperCase(),
          url: String(config?.url ?? ''),
          status: response?.status,
          requestBody: safeStringify(sanitize(config?.data)),
          responseBody: safeStringify(sanitize(response?.data)),
          traceId: traceId ? String(traceId) : undefined,
        },
        MAX_FAILED_REQUESTS
      );
      return Promise.reject(error);
    }
  );
}

/** Vue 组件错误（由 main.ts 的 app.config.errorHandler 调用） */
export function recordVueError(err: unknown, info: string): void {
  const error = err as Error;
  recordError('vue', error?.message ?? safeStringify(err), {
    stack: error?.stack,
    source: info,
  });
}

/** 供「反馈问题」面板展示已采集数量 */
export function getCollectedCount(): { errors: number; failedRequests: number } {
  return { errors: errors.length, failedRequests: failedRequests.length };
}

/** 导出已采集的原始数据（浅拷贝），供提交到后端使用 */
export function getCollectedData(): { errors: CollectedError[]; failedRequests: FailedRequest[] } {
  return {
    errors: errors.map((e) => ({ ...e })),
    failedRequests: failedRequests.map((r) => ({ ...r })),
  };
}

export interface BugEnv {
  orgId: string;
  userId: string;
  userName: string;
  roles: string;
  route: string;
  version: string;
  userAgent: string;
  screen: string;
  language: string;
}

export interface BugExtra {
  description?: string;
  steps?: string;
  screenshot?: string;
}

/** 生成 bug 反馈 .md 内容 */
export function getReport(env: BugEnv, extra: BugExtra = {}): string {
  const time = new Date().toLocaleString();
  const lines: string[] = [];
  lines.push('# 问题反馈报告', '');
  lines.push('## 环境信息');
  lines.push(`- 提交时间：${time}`);
  lines.push(`- 企业（organizationId）：${env.orgId || '-'}`);
  lines.push(`- 用户：${env.userName || '-'}（${env.userId || '-'}）`);
  lines.push(`- 角色：${env.roles || '-'}`);
  lines.push(`- 页面路由：${env.route || '-'}`);
  lines.push(`- 系统版本：${env.version || '-'}`);
  lines.push(`- 浏览器：${env.userAgent || '-'}`);
  lines.push(`- 屏幕分辨率：${env.screen || '-'}`);
  lines.push(`- 语言：${env.language || '-'}`, '');
  lines.push('## 问题描述');
  lines.push(extra.description?.trim() || '（未填写）', '');
  lines.push('## 复现步骤');
  lines.push(extra.steps?.trim() || '（未填写）', '');
  if (extra.screenshot) {
    lines.push('## 截图', `![截图](${extra.screenshot})`, '');
  }
  lines.push('## 最近运行错误');
  if (errors.length === 0) {
    lines.push('（无）', '');
  } else {
    errors.forEach((err) => {
      lines.push(`### [${new Date(err.time).toLocaleTimeString()}] ${err.type}: ${err.message}`);
      if (err.source) {
        const pos = err.lineno != null ? `:${err.lineno}${err.colno != null ? `:${err.colno}` : ''}` : '';
        lines.push(`- 来源：${err.source}${pos}`);
      }
      if (err.stack) {
        lines.push('```', err.stack, '```');
      }
      lines.push('');
    });
  }
  lines.push('## 最近失败接口');
  if (failedRequests.length === 0) {
    lines.push('（无）');
  } else {
    failedRequests.forEach((req) => {
      lines.push(`- ${req.method} ${req.url} → HTTP ${req.status ?? '-'}`);
      if (req.traceId) {
        lines.push(`  - traceId：${req.traceId}`);
      }
      if (req.requestBody) {
        lines.push(`  - 请求参数：${req.requestBody}`);
      }
      if (req.responseBody) {
        lines.push(`  - 响应：${req.responseBody}`);
      }
      lines.push('');
    });
  }
  return lines.join('\n');
}
