import { showFailToast } from 'vant';

import { useI18n } from '@lib/shared/hooks/useI18n';

import useUser from '@/hooks/useUser';

// 被踢下线的时间戳：踢下线后 token 会被立即清空，同页面残留的并发请求会陆续返回空 401，
// 这些「余波」只应保留第一条「已在其他设备登录」提示，后续 401 一律静默，避免「用户没权限」刷屏。
let kickedOutAt = 0;

export default function checkStatus(
  status: number,
  msg: string,
  msgDetail: string | Record<string, any>,
  code?: number,
  noErrorTip?: boolean
): void {
  const { t } = useI18n();
  const { logout, isLoginPage, isWhiteListPage } = useUser();

  let errMessage = '';
  switch (status) {
    case 400:
      errMessage = `${msg}`;
      break;
    case 401: {
      const isKicked = code === 100461;
      // 被踢下线后的余波：第一条 100461 会清空 token，同页面并发请求随后返回的空 401（或再次 100461）
      // 不应再用通用「用户没权限」/重复「已被迫下线」刷屏，静默吞掉，只保留第一条提示。
      if (kickedOutAt && Date.now() - kickedOutAt < 10000) {
        return;
      }
      if (isKicked) {
        kickedOutAt = Date.now();
      }
      // 100461 = 单点登录被踢下线，优先提示「已在其他设备登录」而非通用 401 文案
      errMessage = isKicked ? t('api.errMsgKickedOut') : msg || t('api.errMsg401');
      if (!isLoginPage() && !isWhiteListPage()) {
        logout();
      }
      break;
    }
    case 403:
      errMessage = msg || t('api.errMsg403');
      break;
    // 404请求不存在
    case 404:
      errMessage = msg || t('api.errMsg404');
      break;
    case 405:
      errMessage = msg || t('api.errMsg405');
      break;
    case 408:
      errMessage = msg || t('api.errMsg408');
      break;
    case 500:
      errMessage = msg || t('api.errMsg500');
      break;
    case 501:
      errMessage = msg || t('api.errMsg501');
      break;
    case 502:
      errMessage = msg || t('api.errMsg502');
      break;
    case 503:
      errMessage = msg || t('api.errMsg503');
      break;
    case 504:
      errMessage = msg || t('api.errMsg504');
      break;
    case 505:
      errMessage = msg || t('api.errMsg505');
      break;
    default:
  }
  if (msgDetail && !noErrorTip) {
    if (typeof msgDetail === 'object') {
      errMessage = Object.values(msgDetail)
        .map((e) => e)
        .join('\n');
    } else {
      errMessage = msgDetail;
    }
    showFailToast({
      message: errMessage,
      duration: 5000,
    });
  } else if (errMessage && !noErrorTip) {
    showFailToast({
      message: errMessage,
      duration: 5000,
    });
  }
}
