import mitt, { Handler } from 'mitt';

import { PersonalEnum } from '../enums/systemEnum';

/**
 * 个人中心抽屉跨页面打开通道。
 * 个人中心抽屉全局渲染在 default-layout 中，深层业务组件（如客户画像升级空态）
 * 需要跳转到「个人信息」时通过这里发布事件，避免逐层 emit。
 */
const emitter = mitt();
const OPEN_PERSONAL_INFO = Symbol('OPEN_PERSONAL_INFO');

export function emitOpenPersonalInfo(tab: PersonalEnum) {
  emitter.emit(OPEN_PERSONAL_INFO, tab);
}

export function onOpenPersonalInfo(handler: (tab: PersonalEnum) => void) {
  emitter.on(OPEN_PERSONAL_INFO, handler as Handler);
}

export function offOpenPersonalInfo() {
  emitter.off(OPEN_PERSONAL_INFO);
}
