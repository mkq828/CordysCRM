import { ref } from 'vue';

import type { AgentChatStreamEvent } from '@lib/shared/models/ai';

/** 逐字揭示节奏：每拍字符数与间隔（毫秒），模拟豆包式逐字输出 */
const DEFAULT_CHARS_PER_TICK = 3;
const DEFAULT_TICK_MS = 30;

export interface TypewriteStreamOptions {
  /** 每拍揭示的字符数 */
  charsPerTick?: number;
  /** 每拍间隔（毫秒） */
  tickMs?: number;
  /** 每揭示一段时回调（携带该段文本），用于把逐字内容写入目标 */
  onReveal?: (slice: string) => void;
}

export interface TypewriteOptions {
  /** 每拍揭示的字符数 */
  charsPerTick?: number;
  /** 每拍间隔（毫秒） */
  tickMs?: number;
  /** 每揭示一段时触发（用于跟随滚动等） */
  onTick?: () => void;
}

/** 逐字揭示缓冲器：把外部灌入的 chunk 按固定节奏切片回调出去 */
interface RevealLoop {
  feed: (chunk: string) => void;
  flush: () => void;
  finish: () => Promise<void>;
  reset: () => void;
}

function createRevealLoop(onReveal: (slice: string) => void, charsPerTick: number, tickMs: number): RevealLoop {
  let pending = '';
  let ended = false;
  let timerId: number | undefined;
  let resolveDrain: (() => void) | undefined;

  function stop() {
    if (timerId !== undefined) {
      window.clearInterval(timerId);
      timerId = undefined;
    }
  }

  function tick() {
    if (pending.length > 0) {
      const slice = pending.slice(0, charsPerTick);
      pending = pending.slice(charsPerTick);
      onReveal(slice);
    } else if (ended) {
      stop();
      resolveDrain?.();
      resolveDrain = undefined;
    }
  }

  function feed(chunk: string) {
    if (!chunk) {
      return;
    }
    pending += chunk;
    if (timerId === undefined) {
      timerId = window.setInterval(tick, tickMs);
    }
  }

  function flush() {
    if (pending.length > 0) {
      const slice = pending;
      pending = '';
      onReveal(slice);
    }
  }

  function finish(): Promise<void> {
    ended = true;
    if (pending.length === 0) {
      stop();
      return Promise.resolve();
    }
    if (timerId === undefined) {
      timerId = window.setInterval(tick, tickMs);
    }
    return new Promise<void>((resolve) => {
      resolveDrain = resolve;
    });
  }

  function reset() {
    stop();
    pending = '';
    ended = false;
    resolveDrain = undefined;
  }

  return { feed, flush, finish, reset };
}

/**
 * 消费一条流式事件流，把 chunk 逐字揭示出来，返回完整文本。
 * 出错时先把已缓冲内容补齐再抛错，避免丢字。
 * 适用于需要在多个独立对象上分别逐字揭示的场景（如会议室的每一条发言）。
 */
export async function typewriteStream(
  stream: AsyncIterable<AgentChatStreamEvent>,
  options: TypewriteStreamOptions = {}
): Promise<string> {
  const { charsPerTick = DEFAULT_CHARS_PER_TICK, tickMs = DEFAULT_TICK_MS, onReveal } = options;
  let full = '';
  const loop = createRevealLoop(
    (slice) => {
      full += slice;
      onReveal?.(slice);
    },
    charsPerTick,
    tickMs
  );

  let streamError: Error | undefined;
  try {
    // eslint-disable-next-line no-restricted-syntax -- 原生 async generator，顺序消费流式事件
    for await (const event of stream) {
      if (event.type === 'chunk') {
        loop.feed(event.content || '');
      } else if (event.type === 'error') {
        streamError = new Error(event.errorMessage);
        break;
      }
    }
  } catch (error) {
    streamError = error as Error;
  }

  if (streamError) {
    loop.flush();
    loop.reset();
    throw streamError;
  }

  await loop.finish();
  loop.reset();
  return full;
}

/**
 * 单实例逐字输出组合式函数：内部维护一个 text 响应式变量，
 * 调用方逐个 feed chunk，finish 返回「缓冲揭示完毕」的 Promise。
 * 适用于单个组件内只有一处流式文本的场景。
 */
export default function useTypewriter(options: TypewriteOptions = {}) {
  const { charsPerTick = DEFAULT_CHARS_PER_TICK, tickMs = DEFAULT_TICK_MS, onTick } = options;
  const text = ref('');
  const isTyping = ref(false);

  const loop = createRevealLoop(
    (slice) => {
      text.value += slice;
      onTick?.();
    },
    charsPerTick,
    tickMs
  );

  function feed(chunk: string) {
    loop.feed(chunk);
    isTyping.value = true;
  }

  function flush() {
    loop.flush();
  }

  function finish(): Promise<void> {
    return loop.finish().finally(() => {
      isTyping.value = false;
    });
  }

  function reset() {
    loop.reset();
    text.value = '';
    isTyping.value = false;
  }

  return { text, isTyping, feed, flush, finish, reset };
}
